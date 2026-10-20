// AI 封面生成中转云函数
// 背景：小程序成长计划二期(2026-07~12)赠送的 10 万张图像额度仅限「小程序 SDK / 云函数」通道消耗，
//       Java 后端直连不可用；因此后端通过 HTTP 访问调用本函数，本函数用 wx-server-sdk 调混元
//       HY-Image-3.0（文生图），合法消耗赠送额度。
// 鉴权：与 llm-proxy 共用 process.env.PROXY_KEY，校验请求头 x-proxy-key。
// 合规：官方文档未提供 LogoAdd 开关，改为显式传 footnote（≤ 16 字）在结果图右下角烧入水印，
//       满足 GB 45438-2025 对 AI 生成图片的显式标识要求；本函数不做去水印。
// 返回：混元生图实际返回的是临时图片 URL（res.data[0].url），为让前端可写临时文件展示与上传，
//       本函数统一下载后转成 base64 dataURL（{ imageBase64 }）。
const cloud = require('wx-server-sdk');
const https = require('https');
const http = require('http');

// 生图耗时远高于文本（15–60s 常见），超时拉到 120s；后端 http 需 ≥ 此值
cloud.init({ env: cloud.DYNAMIC_CURRENT_ENV, timeout: 120000 });

// 官方：生图走 createImageModel('hunyuan-image')，与生文的 createModel('cloudbase') 不是同一个入口
const IMAGE_PROVIDER = 'hunyuan-image';
const MODEL = 'HY-Image-3.0-Plus-4090-Tob-v1.0';
// 混元 size 格式为「宽x高」，可用：1024x1024 / 1280x720 / 720x1280 / 1280x1280
const DEFAULT_SIZE = '1024x1024';
// 右下角自定义水印（合规显式标识），官方限制 16 个字符以内
const FOOTNOTE = 'AI 生成';
// 并发超限重试：官方 FAQ 建议 3–5s 退避；这里 4s 重试 1 次仍失败则抛
const RETRY_DELAY_MS = 4000;

/**
 * 从 HTTP 触发或普通调用中提取请求体
 */
function parseInput(event) {
  // HTTP 触发：body 可能是 base64
  if (typeof event.body === 'string' && event.body.length > 0) {
    const raw = event.isBase64Encoded
      ? Buffer.from(event.body, 'base64').toString('utf-8')
      : event.body;
    try {
      return JSON.parse(raw);
    } catch (e) {
      return {};
    }
  }
  // 普通 callFunction：参数即 event 本身
  return event || {};
}

/**
 * 大小写不敏感取请求头
 */
function getHeader(event, key) {
  const headers = event.headers || {};
  const target = key.toLowerCase();
  const hit = Object.keys(headers).find((k) => k.toLowerCase() === target);
  return hit ? headers[hit] : undefined;
}

// 是否为 HTTP 触发（存在 httpMethod）
const isHttp = (event) => !!event.httpMethod;

// 统一响应封装：HTTP 触发返回网关结构，普通调用返回纯对象
function respond(event, statusCode, obj) {
  const body = JSON.stringify(obj);
  if (isHttp(event)) {
    return {
      statusCode,
      headers: { 'content-type': 'application/json; charset=utf-8' },
      isBase64Encoded: false,
      body,
    };
  }
  return obj;
}

// 简单延时
const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

/**
 * size 容错归一：后端旧配置可能传「1024:1024」，混元只认「1024x1024」
 */
function normalizeSize(size) {
  const s = (typeof size === 'string' && size.trim()) || DEFAULT_SIZE;
  return s.replace(/[:\*\/]/g, 'x');
}

/**
 * 从 generateImage 返回结构中尽力提取图片。
 * 官方文档：正常形态为 res.data[0].url（临时图片链接），部分形态带 b64_image；
 * 兼容取法：data[0].url / data[0].b64_json / image / b64_image / base64 / images[0] / output[0]。
 * 返回 { kind: 'base64' | 'url', value }；未识别返回 null。
 */
function extractImage(result) {
  if (!result) return null;
  const first = (Array.isArray(result.data) && result.data[0]) || null;
  const candidates = [
    first && (first.url || first.b64_image || first.b64_json),
    result.image,
    result.b64_image,
    result.base64,
    result.images && result.images[0],
    result.output && result.output[0],
  ].filter((v) => typeof v === 'string' && v.length > 0);
  for (const v of candidates) {
    if (v.startsWith('data:')) return { kind: 'base64', value: v };
    if (v.startsWith('http://') || v.startsWith('https://')) return { kind: 'url', value: v };
    // 纯 base64 串，按头部字符推真实 MIME 后补 dataURL 前缀
    const mime = v.indexOf('/9j/') === 0 ? 'image/jpeg'
      : v.indexOf('iVBOR') === 0 ? 'image/png'
      : v.indexOf('R0lGOD') === 0 ? 'image/gif'
      : v.indexOf('UklGR') === 0 ? 'image/webp' : 'image/jpeg';
    return { kind: 'base64', value: 'data:' + mime + ';base64,' + v };
  }
  return null;
}

/**
 * 按文件头魔数识别图片真实 MIME。实测混元返回的 URL 带 .png 命名、响应头也报 image/png，
 * 但字节其实是 JPEG（base64 以 /9j/ 开头）；以后缀/头部为准会让七牛外链 Content-Type 与内容不一致，故以魔数为准。
 */
function sniffMime(buf, fallback) {
  if (!buf || buf.length < 4) return fallback;
  if (buf[0] === 0xff && buf[1] === 0xd8 && buf[2] === 0xff) return 'image/jpeg';
  if (buf[0] === 0x89 && buf[1] === 0x50 && buf[2] === 0x4e && buf[3] === 0x47) return 'image/png';
  if (buf.toString('ascii', 0, 3) === 'GIF') return 'image/gif';
  if (buf.toString('ascii', 0, 4) === 'RIFF' && buf.toString('ascii', 8, 12) === 'WEBP') return 'image/webp';
  return fallback;
}

/**
 * 拉远程图片 URL 转 base64。生图模型如返回临时 URL，需在此转成 dataURL
 * 让后端只透传，前端 writeFile 后走现有 uploadMedia。
 */
function fetchAsDataUrl(url) {
  return new Promise((resolve, reject) => {
    const mod = url.startsWith('https') ? https : http;
    const req = mod.get(url, (res) => {
      // 处理可能的 3xx 重定向（临时 URL 常见）
      if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
        res.resume();
        return resolve(fetchAsDataUrl(res.headers.location));
      }
      if (res.statusCode !== 200) {
        res.resume();
        return reject(new Error('download image failed, status=' + res.statusCode));
      }
      const chunks = [];
      res.on('data', (c) => chunks.push(c));
      res.on('end', () => {
        const headerType = (res.headers['content-type'] || 'image/jpeg').split(';')[0];
        const buffer = Buffer.concat(chunks);
        resolve('data:' + sniffMime(buffer, headerType) + ';base64,' + buffer.toString('base64'));
      });
      res.on('error', reject);
    });
    req.on('error', reject);
    req.setTimeout(30000, () => req.destroy(new Error('download image timeout')));
  });
}

/**
 * 判断错误是否为并发超限（官方文档给的错误码），需重试
 */
function isConcurrencyError(err) {
  if (!err) return false;
  const msg = String(err.message || err.code || err.errMsg || '');
  return msg.includes('EXCEED_CONCURRENT_REQUEST_LIMIT') || msg.includes('concurrent');
}

exports.main = async (event) => {
  const expectedKey = process.env.PROXY_KEY;
  const gotKey = getHeader(event, 'x-proxy-key');
  if (!expectedKey || gotKey !== expectedKey) {
    return respond(event, 401, { error: 'unauthorized' });
  }

  const { prompt, size, model } = parseInput(event);
  if (!prompt || typeof prompt !== 'string' || !prompt.trim()) {
    return respond(event, 400, { error: 'prompt is required' });
  }

  const ai = cloud.ai();
  // SDK 版本过旧时 createImageModel 不存在，先探探并回带可用方法名，避免只报个冷冰冰的 not a function
  if (typeof ai.createImageModel !== 'function') {
    const methods = Object.keys(Object.getPrototypeOf(ai) || {}).concat(Object.keys(ai));
    console.error('image-proxy createImageModel unavailable, ai methods:', methods);
    return respond(event, 500, {
      error: 'wx-server-sdk 不支持 createImageModel，请升级依赖（云端安装依赖）；当前可用: ' + methods.join(','),
    });
  }
  const imageModel = ai.createImageModel(IMAGE_PROVIDER);
  const params = {
    // 后端可下传 model 覆盖，模型下线时不必重新部署本函数
    model: (typeof model === 'string' && model.trim()) || MODEL,
    prompt: prompt.trim(),
    size: normalizeSize(size),
    // 官方未提供 LogoAdd，用 footnote 显式保证右下角 AI 标识
    footnote: FOOTNOTE,
    // revise 默认关：本服务 prompt 已含固定风格锚与禁止项，不让模型改写以免丟约束
  };

  let result;
  try {
    result = await imageModel.generateImage(params);
  } catch (err) {
    // 并发超限：等 4s 重试一次；其他错误直接抛
    if (isConcurrencyError(err)) {
      console.warn('image-proxy concurrency limit, retry once:', err && err.message);
      await sleep(RETRY_DELAY_MS);
      try {
        result = await imageModel.generateImage(params);
      } catch (err2) {
        console.error('image-proxy retry failed:', err2);
        return respond(event, 503, {
          error: (err2 && (err2.message || err2.errMsg)) || 'generate image failed after retry',
          code: 'EXCEED_CONCURRENT_REQUEST_LIMIT',
        });
      }
    } else {
      console.error('image-proxy generateImage failed:', err);
      return respond(event, 500, {
        error: (err && (err.message || err.errMsg)) || 'generate image failed',
      });
    }
  }

  const extracted = extractImage(result);
  if (!extracted) {
    const dump = JSON.stringify(result || {}).slice(0, 300);
    console.error('image-proxy unrecognized response shape:', dump);
    // 把原始结构片段回带给后端日志，首次接入时免去云函数看日志
    return respond(event, 500, { error: 'generate image response unrecognized: ' + dump });
  }

  try {
    const imageBase64 = extracted.kind === 'base64'
      ? extracted.value
      : await fetchAsDataUrl(extracted.value);
    // 与后端 AiImageVO 字段对齐：{ imageBase64: "data:image/png;base64,..." }
    return respond(event, 200, { imageBase64 });
  } catch (err) {
    console.error('image-proxy download image failed:', err);
    return respond(event, 500, { error: 'download generated image failed: ' + (err.message || '') });
  }
};

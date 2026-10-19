// AI 荐菜 LLM 中转云函数
// 背景：小程序成长计划赠送的 AI 额度仅限「小程序 SDK / 云函数」通道消耗，后端直连不可用，
//       故 Java 后端通过 HTTP 访问调用本函数，本函数用 wx-server-sdk 调混元 hy3，合法消耗赠送额度。
// 鉴权：与后端共享密钥，校验请求头 x-proxy-key === process.env.PROXY_KEY。
const cloud = require('wx-server-sdk');

// AI 生成耗时较长，超时放宽到 60s（后端 http 超时需 ≥ 此值）
cloud.init({ env: cloud.DYNAMIC_CURRENT_ENV, timeout: 60000 });

const MODEL = 'hy3';

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

exports.main = async (event) => {
  const expectedKey = process.env.PROXY_KEY;
  const gotKey = getHeader(event, 'x-proxy-key');
  if (!expectedKey || gotKey !== expectedKey) {
    return respond(event, 401, { error: 'unauthorized' });
  }

  const { messages, temperature } = parseInput(event);
  if (!Array.isArray(messages) || messages.length === 0) {
    return respond(event, 400, { error: 'messages is required' });
  }

  try {
    const ai = cloud.ai();
    const model = ai.createModel('cloudbase');
    const params = { model: MODEL, messages };
    if (typeof temperature === 'number') {
      params.temperature = temperature;
    }
    const result = await model.generateText(params);
    const content = (result && (result.text
      || (result.choices && result.choices[0] && result.choices[0].message && result.choices[0].message.content))) || '';
    // 转成 OpenAI chat/completions 兼容结构，后端 LlmClient 按此解析
    return respond(event, 200, {
      choices: [{ index: 0, message: { role: 'assistant', content }, finish_reason: 'stop' }],
      usage: (result && result.usage) || undefined,
    });
  } catch (error) {
    console.error('llm-proxy error:', error);
    return respond(event, 500, { error: (error && error.message) || 'llm-proxy error' });
  }
};

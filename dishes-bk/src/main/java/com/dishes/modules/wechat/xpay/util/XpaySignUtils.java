package com.dishes.modules.wechat.xpay.util;

import com.dishes.exception.RunException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * 微信虚拟支付签名（docs/v12 §2.3 / §9.1）
 * <p>
 * 两套签名都是 HMAC-SHA256 转小写十六进制，但**参与签名的原文不同**，这是整条支付链路上最容易写错的地方：
 * <pre>
 * paySig    = hex(hmac_sha256(appKey,     uri + '&' + body))   // 支付签名，C 端 uri 固定为 requestVirtualPayment，
 *                                                                // B 端 uri 为接口路径且不带 ?query
 * signature = hex(hmac_sha256(sessionKey, body))               // 用户态签名，不含 uri
 * </pre>
 * 也就是说客户端下单那对：paySig 里有 {@code "requestVirtualPayment&"} 前缀，signature 里没有。
 * </p>
 * <p>
 * ⚠️ 传进来的 {@code body} 必须与真正发出去的请求体**逐字节一致**：调用方应当先把 body 序列化成字符串、
 * 再用同一个字符串既签名又发送，绝不能签一次、发送时让框架再序列化一次（键顺序或空格一变，微信侧就是 -15007/268490003）。
 * 本类因此不接收对象、只接收已经序列化好的字符串。
 * </p>
 * <p>
 * 本类不含任何业务语义（不认识会员也不认识道具），所以放在通道包下，任何虚拟商品都复用它。
 * </p>
 *
 * @author 好市民
 */
public class XpaySignUtils {

    private static final String ALGORITHM = "HmacSHA256";
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private XpaySignUtils() {
    }

    /**
     * 支付签名 paySig
     *
     * @param uri      接口路径，不带问号与参数（C 端下单固定传 requestVirtualPayment）
     * @param postBody 实际发出的请求体原文
     * @param appKey   虚拟支付 AppKey，须与 env 同环境（沙箱/现网不通用）
     * @return 小写十六进制
     */
    public static String buildPaySig(String uri, String postBody, String appKey) {
        return hmacSha256Hex(appKey, uri + "&" + postBody);
    }

    /**
     * 用户态签名 signature（只对请求体签名，不拼 uri）
     *
     * @param postBody   实际发出的请求体原文
     * @param sessionKey 登录时 auth.code2Session 拿到的会话密钥
     * @return 小写十六进制
     */
    public static String buildSignature(String postBody, String sessionKey) {
        return hmacSha256Hex(sessionKey, postBody);
    }

    /**
     * HMAC-SHA256 → 小写十六进制
     *
     * @param key  密钥
     * @param data 原文
     * @return
     */
    public static String hmacSha256Hex(String key, String data) {
        if (key == null || data == null) {
            throw new RunException("签名参数缺失");
        }
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return toHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            // JDK 不可能不支持 HmacSHA256，走到这里基本是密钥串本身有问题（如 AppKey 配成空），
            // 但异常在钱的路径上必须显式失败，不能退化成"签个空串发出去"
            throw new RunException("虚拟支付签名计算失败");
        }
    }

    /**
     * 自己拼十六进制而不是 DatatypeConverter：后者在 Java 9+ 被移出 JDK，
     * 而本项目 pom 锁 1.8，留一个不依赖额外 artifact 的写法省掉将来升级时的麻烦
     */
    private static String toHex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            out[i * 2] = HEX[v >>> 4];
            out[i * 2 + 1] = HEX[v & 0x0F];
        }
        return new String(out);
    }
}

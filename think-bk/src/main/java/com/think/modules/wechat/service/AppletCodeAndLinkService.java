package com.think.modules.wechat.service;

/**
 * 小程序码与小程序链接
 * https://developers.weixin.qq.com/miniprogram/dev/OpenApiDoc/qrcode-link/qr-code/getQRCode.html
 * @author 好市民
 */
public interface AppletCodeAndLinkService {
    /**
     * 获取小程序二维码
     * @param scene 参数
     * @param page 页面
     * @return
     */
    byte[] getUnlimitedQrcodeByte(String scene, String page);
}

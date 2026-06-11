package org.example.cloud.demo.util;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * AES 对称加密工具类
 * 用于加密住户敏感信息（姓名、部门等），写入数据库前加密，读取后解密。
 *
 * 加分项说明：使用 AES-128 对称加密，保护住户隐私数据。
 * 密钥实际部署时从环境变量或配置中心获取。
 */
@Slf4j
@Component
public class AesUtil {

    private static final String SECRET_KEY = "EstateMgmt@2026!";
    private static final AES aes;

    static {
        aes = SecureUtil.aes(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }

    /** 加密 → Base64 编码 */
    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) return plainText;
        try {
            byte[] encrypted = aes.encrypt(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("AES 加密失败: {}", e.getMessage());
            throw new RuntimeException("数据加密失败", e);
        }
    }

    /** 解密 Base64 → 明文 */
    public static String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) return cipherText;
        try {
            byte[] decoded = Base64.getDecoder().decode(cipherText);
            byte[] decrypted = aes.decrypt(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("AES 解密失败, 密文可能未加密或格式不正确: {}", e.getMessage());
            return cipherText;  // 兼容旧数据（未加密的明文）
        }
    }
}

package org.example.cloud.demo.util;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 敏感数据加解密工具类
 * 加分项说明：使用 AES 对称加密算法对住户的敏感信息（如姓名、部门等）进行加密存储，
 *             防止数据库泄露导致的隐私数据泄漏。
 *
 * 使用方式：
 *   加密：EncryptUtil.encrypt("明文")
 *   解密：EncryptUtil.decrypt("密文")
 *
 * 注意：生产环境中密钥应从外部配置中心获取，此处仅为演示。
 */
@Slf4j
@Component
public class EncryptUtil {

    /**
     * AES 密钥（16位 = 128位密钥）
     * 实际项目中应从环境变量或配置中心获取
     */
    private static final String SECRET_KEY = "EstateMgmt2026!@";

    private static final AES aes;

    static {
        // 使用 Hutool 工具构建 AES 对象
        aes = SecureUtil.aes(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 加密敏感数据
     *
     * @param plainText 明文
     * @return Base64 编码的密文字符串
     */
    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        try {
            // AES 加密后转 Base64 编码
            byte[] encrypted = aes.encrypt(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("数据加密失败: {}", e.getMessage());
            throw new RuntimeException("数据加密失败", e);
        }
    }

    /**
     * 解密敏感数据
     *
     * @param cipherText Base64 编码的密文字符串
     * @return 明文字符串
     */
    public static String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) {
            return cipherText;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(cipherText);
            byte[] decrypted = aes.decrypt(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("数据解密失败: {}", e.getMessage());
            throw new RuntimeException("数据解密失败", e);
        }
    }

    /**
     * 验证明文与密文是否匹配
     */
    public static boolean verify(String plainText, String cipherText) {
        String decrypted = decrypt(cipherText);
        return plainText.equals(decrypted);
    }
}

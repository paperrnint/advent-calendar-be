package com.example.adventcalendar.util;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class EncryptionUtils {

	private static final String ALGORITHM = "AES/GCM/NoPadding";
	private static final int GCM_IV_LENGTH = 12;
	private static final int GCM_TAG_LENGTH = 128;

	private static SecretKeySpec secretKeyStatic;

	@Value("${encryption.secret-key}")
	private String secretKey;

	@PostConstruct
	public void init() {
		byte[] keyBytes = secretKey.getBytes();
		if (keyBytes.length != 32) {
			throw new IllegalArgumentException("암호화 키는 32바이트여야 합니다");
		}
		secretKeyStatic = new SecretKeySpec(keyBytes, "AES");
	}

	public static String encrypt(String plainText) {
		if (plainText == null) {
			return null;
		}

		try {
			byte[] iv = new byte[GCM_IV_LENGTH];
			new SecureRandom().nextBytes(iv);

			Cipher cipher = Cipher.getInstance(ALGORITHM);
			GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
			cipher.init(Cipher.ENCRYPT_MODE, secretKeyStatic, parameterSpec);

			byte[] encryptedBytes = cipher.doFinal(plainText.getBytes());

			ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encryptedBytes.length);
			byteBuffer.put(iv);
			byteBuffer.put(encryptedBytes);

			return Base64.getEncoder().encodeToString(byteBuffer.array());
		} catch (Exception e) {
			throw new RuntimeException("암호화 실패", e);
		}
	}

	public static String decrypt(String encryptedText) {
		if (encryptedText == null) {
			return null;
		}

		try {
			byte[] decoded = Base64.getDecoder().decode(encryptedText);

			ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
			byte[] iv = new byte[GCM_IV_LENGTH];
			byteBuffer.get(iv);
			byte[] encryptedBytes = new byte[byteBuffer.remaining()];
			byteBuffer.get(encryptedBytes);

			Cipher cipher = Cipher.getInstance(ALGORITHM);
			GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
			cipher.init(Cipher.DECRYPT_MODE, secretKeyStatic, parameterSpec);

			return new String(cipher.doFinal(encryptedBytes));
		} catch (Exception e) {
			throw new RuntimeException("복호화 실패", e);
		}
	}

	// 테스트용 초기화 메서드
	public static void initForTest(String key) {
		byte[] keyBytes = key.getBytes();
		secretKeyStatic = new SecretKeySpec(keyBytes, "AES");
	}
}

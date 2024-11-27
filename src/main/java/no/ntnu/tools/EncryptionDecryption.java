package no.ntnu.tools;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

/**
 * A class for encrypting and decrypting messages.
 */
public class EncryptionDecryption {
  private static final String ALGORITHM = "AES";

  /**
   * Generate a secret key for encryption and decryption.
   *
   * @return A secret key or {@code null} if the key could not be generated.
   */
  public static SecretKey generateKey() {
    try {
      SecretKey key = null;
      KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
      keyGenerator.init(256);
      key = keyGenerator.generateKey();
      return key;
    } catch (NoSuchAlgorithmException e) {
      Logger.error("Failed to generate key: " + e.getMessage());
      return null;
    }
  }

  /**
   * Encrypt a message using a secret key.
   *
   * @param message The message to encrypt
   * @param key     The secret key to use for encryption
   * @return The encrypted message or {@code null} if the encryption failed.
   * @throws NoSuchAlgorithmException  If the encryption algorithm is not available
   * @throws NoSuchPaddingException    If the padding scheme is not available
   * @throws InvalidKeyException       If the key is invalid
   * @throws IllegalBlockSizeException If the block size is invalid
   * @throws BadPaddingException       If the padding is invalid
   */
  public static String encrypt(String message, SecretKey key) throws NoSuchAlgorithmException,
      NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
    String encryptedMessage = null;

    Cipher cipher = Cipher.getInstance(ALGORITHM);
    cipher.init(Cipher.ENCRYPT_MODE, key);
    byte[] encrypted = cipher.doFinal(message.getBytes());
    encryptedMessage = Base64.getEncoder().encodeToString(encrypted);

    return encryptedMessage;
  }

  /**
   * Decrypt a message using a secret key.
   *
   * @param encryptedMessage The message to decrypt
   * @param key              The secret key to use for decryption
   * @return The decrypted message or {@code null} if the decryption failed.
   * @throws NoSuchPaddingException    If the padding scheme is not available
   * @throws NoSuchAlgorithmException  If the encryption algorithm is not available
   * @throws InvalidKeyException       If the key is invalid
   * @throws IllegalBlockSizeException If the block size is invalid
   * @throws BadPaddingException       If the padding is invalid
   */
  public static String decrypt(String encryptedMessage, SecretKey key)
      throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException,
      IllegalBlockSizeException, BadPaddingException {
    String decryptedMessage = null;

    Cipher cipher = Cipher.getInstance(ALGORITHM);
    cipher.init(Cipher.DECRYPT_MODE, key);
    byte[] decodedBytes = Base64.getDecoder().decode(encryptedMessage);
    byte[] decrypted = cipher.doFinal(decodedBytes);
    decryptedMessage = new String(decrypted);

    return decryptedMessage;
  }
}

package no.ntnu.tools;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A class for calculating checksums of data.
 */
public class ChecksumHandler {
  /**
   * Calculate the checksum of the given data.
   *
   * @param data The data to calculate the checksum of
   * @return The checksum of the data
   * @throws NoSuchAlgorithmException If the checksum algorithm is not available
   */
  public String calculateChecksum(String data) throws NoSuchAlgorithmException {
    MessageDigest digest = MessageDigest.getInstance("SHA-256"); // Use SHA-256 for checksum
    byte[] hash = digest.digest(data.getBytes()); // Calculate the hash
    StringBuilder hexString = new StringBuilder(); // Convert the hash to a hex string
    for (byte b : hash) {
      String hex = Integer.toHexString(0xff & b);
      if (hex.length() == 1) {
        hexString.append('0');
      }
      hexString.append(hex);
    }
    return hexString.toString();
  }
}

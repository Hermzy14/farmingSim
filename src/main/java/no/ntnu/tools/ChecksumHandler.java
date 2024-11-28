package no.ntnu.tools;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A class for calculating checksums of data.
 * This class was created with help from GitHub Copilot.
 * Code was modified to fit the project.
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
    // Use SHA-256 for checksum
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    // Calculate the hash
    byte[] hash = digest.digest(data.getBytes());
    // Convert the hash to a hex string
    StringBuilder hexString = new StringBuilder();
    for (byte b : hash) {
      String hex = Integer.toHexString(0xff & b);
      if (hex.length() == 1) {
        hexString.append('0');
      }
      hexString.append(hex);
    }
    // Return the hex string
    return hexString.toString();
  }
}

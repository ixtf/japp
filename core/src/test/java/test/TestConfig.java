package test;

import lombok.SneakyThrows;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;

import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

public class TestConfig {
  @SneakyThrows
  public static void main(String[] args) {
    final var sha256 = MessageDigest.getInstance("sha256");
    final var digest = sha256.digest("123".getBytes(StandardCharsets.UTF_8));
    final var x = Hex.toHexString(digest);
    System.out.println(x);
    System.out.println(x.length());

//    testRSA();
//    testSM2();
  }

  @SneakyThrows
  private static void testSM2() {
    final var sm2Spec = new ECGenParameterSpec("sm2p256v1");
//    final var sm2 = KeyPairGenerator.getInstance("EC","BC");
    final var sm2 = KeyPairGenerator.getInstance("EC", new BouncyCastleProvider());
    sm2.initialize(sm2Spec);
    sm2.initialize(sm2Spec, new SecureRandom());
    final var keyPair = sm2.generateKeyPair();
    final var key = keyPair.getPrivate();
    final var encoded = key.getEncoded();
    final var s = Base64.getEncoder().encodeToString(encoded);
    // MIGTAgEAMBMGByqGSM49AgEGCCqBHM9VAYItBHkwdwIBAQQgZGJlgMxns6L9+65fU6epNsq1w5rH+POXt4S1f7+5XjugCgYIKoEcz1UBgi2hRANCAASRW/Qq53FON6VclWcqw9x+YjjYtUgewCNZUBLYqjiVx0HCQYaN/XzW3Uy2ygkIOei407yE/M4Gy46HNT9gByL7
    System.out.println(s);
  }

  @SneakyThrows
  private static void testRSA() {
    final var rsa = KeyPairGenerator.getInstance("RSA");
    final var keyPair = rsa.generateKeyPair();
    final var key = keyPair.getPrivate();
    final var encoded = key.getEncoded();
    final var s = Base64.getEncoder().encodeToString(encoded);
    System.out.println(s);
  }
}

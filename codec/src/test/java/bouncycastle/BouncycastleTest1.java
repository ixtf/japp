package bouncycastle;

import lombok.SneakyThrows;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PrivateKeyInfoFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * @author jzb 2019-05-04
 */
public class BouncycastleTest1 {
    @SneakyThrows
    public static void main(String[] args) {
        RSAKeyPairGenerator rsaKeyPairGenerator = new RSAKeyPairGenerator();
        RSAKeyGenerationParameters rsaKeyGenerationParameters = new RSAKeyGenerationParameters(BigInteger.valueOf(3), new SecureRandom(), 1024, 25);
        rsaKeyPairGenerator.init(rsaKeyGenerationParameters);//初始化参数
        AsymmetricCipherKeyPair keyPair = rsaKeyPairGenerator.generateKeyPair();

        AsymmetricKeyParameter publicKey = keyPair.getPublic();//公钥
        AsymmetricKeyParameter privateKey = keyPair.getPrivate();//私钥

        SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(publicKey);
        PrivateKeyInfo privateKeyInfo = PrivateKeyInfoFactory.createPrivateKeyInfo(privateKey);

        //变字符串
        ASN1Object asn1ObjectPublic = subjectPublicKeyInfo.toASN1Primitive();
        byte[] publicInfoByte = asn1ObjectPublic.getEncoded();
        ASN1Object asn1ObjectPrivate = privateKeyInfo.toASN1Primitive();
        byte[] privateInfoByte = asn1ObjectPrivate.getEncoded();

        //这里可以将密钥对保存到本地
        final Base64.Encoder encoder64 = Base64.getEncoder();
        // MIGdMA0GCSqGSIb3DQEBAQUAA4GLADCBhwKBgQDh+LXtxwNS4UGmyIuQ0KNnoi5tbXZEmrObEdKnaj+ttaold1x44RqQ2yAowypRhdXi4Gd14C/7D3dgFq6wnFaI9Z46gzZEmgqtmVqNB2PvPfshlpIdcbP7LTAjU7heYCtR5kRkQH8T79TqxxtA8fF9LdvSfC+ZnEKpvaNrJa3JcQIBAw==
        // MIGdMA0GCSqGSIb3DQEBAQUAA4GLADCBhwKBgQCy5C9I2tzpc/2lXmgNNGus1bhE38p7mfZAK6z50cyxjIoHJNi2nCicXHFW/iqBilgItpF4Y8PLVEFXUa/0Qxf/cDUdeCoEECqCsYgTGU8o6yBOGh1+KVTdTcENAsbYPjVTNCdpvMGoW1Oey6MeggY3cGVgpo3/bEKbTCPO3AE0GwIBAw==
        System.out.println("PublicKey:\n" + encoder64.encodeToString(publicInfoByte));
        System.out.println("PrivateKey:\n" + encoder64.encodeToString(privateInfoByte));
    }

    //加密
    public static String encryptData(String data) throws IOException, InvalidCipherTextException {
        data = String.valueOf(System.currentTimeMillis()) + "::" + data;
        final Base64.Decoder decoder64 = Base64.getDecoder();
        final Base64.Encoder encoder64 = Base64.getEncoder();

        AsymmetricBlockCipher cipher = new RSAEngine();

        //加密
        String publicInfoStr = "MIGdMA0GCSqGSIb3DQEBAQUAA4GLADCBhwKBgQDNgn9X76T8t0gba+JMe686ynress+0Pd3LH9qqft8ZNC+d6BG3trc3rrn83W+q12pH+YfWiDsTB9jpeTg0EixQX4s9WrPnhOG289nzzwDrmuWMRS9KUSOwq4o3ymKlKnOIz2ncOzOsfLAPnnF7FZAJRZTkyitoYywk5RyIqvLhUwIBAw==";
        byte[] publicInfoBytes = decoder64.decode(publicInfoStr);

        ASN1Object pubKeyObj = ASN1Primitive.fromByteArray(publicInfoBytes); //这里也可以从流中读取，从本地导入
        AsymmetricKeyParameter pubKey = PublicKeyFactory.createKey(SubjectPublicKeyInfo.getInstance(pubKeyObj));

        cipher.init(true, pubKey);//true表示加密
        byte[] encryptDataBytes = cipher.processBlock(data.getBytes("utf-8")
                , 0, data.getBytes("utf-8").length);
        String encryptData = encoder64.encodeToString(encryptDataBytes);
        return encryptData;
    }

    //解密
    public static String decryptData(String data) throws IOException, InvalidCipherTextException {
        final Base64.Decoder decoder64 = Base64.getDecoder();

        AsymmetricBlockCipher cipher = new RSAEngine();
        byte[] encryptDataBytes = decoder64.decode(data);

        //解密
        String privateInfoStr = "MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBAM2Cf1fvpPy3SBtr4kx7rzrKet6yz7Q93csf2qp+3xk0L53oEbe2tzeuufzdb6rXakf5h9aIOxMH2Ol5ODQSLFBfiz1as+eE4bbz2fPPAOua5YxFL0pRI7CrijfKYqUqc4jPadw7M6x8sA++k9y/5xxUlLt4H76atnp5z3p0e/3o9RyPnC/7r+Ra0gyv5fD7es1hct8MF6ITu9JEmMOW0ETzx7eXuCUt4O6DqA1vHacRId1EsTUUemcHrNba/PudtxriuzfNA6aMZ4UtaFbf0nFyn4nLAkEA/3dsusv0Xd70cwBa5wM69GgsLZWvYxevZCZ552b1LpjYsAk5iy95SspJbM6S10k6gbTazY5+KucoItiDekxQqwJBAM3wXYJON8AdnR6hMXpSHULpKFLINSiP7SC3Q0lI5BTQ4ICpCCSA8OlqTMUQP+ot+xBakCoBonQoenKIWwS3QfkCQQCqT53R3U2T6fhMqudErNH4RXLJDnTsunTtbvvvmfjJuzsgBiZcylDchtud3wyPhicBIzyJCalx73AXOwJRiDXHAkEAiUrpAYl6gBO+FGt2UYwTgfDFjIV4xbVIwHos24XtYzXrAHCwGFX18PGIg2AqnB6nYDxgHAEW+Br8TFrnWHor+wJBAMMkThUMH16vOYbSHaKMh518UX/pJ3Y4uSHjIe0DDRtKiYy5Z9QtoA/nae5jBfDsp2o5wuzfto0p3BH0i9D+AVQ=";
        byte[] privateInfoByte = decoder64.decode(privateInfoStr);
        AsymmetricKeyParameter priKey = PrivateKeyFactory.createKey(privateInfoByte);
        cipher.init(false, priKey);//false表示解密

        byte[] decryptDataBytes = cipher.processBlock(encryptDataBytes, 0, encryptDataBytes.length);
        String decryptData = new String(decryptDataBytes, "utf-8");
        return decryptData;
    }

}

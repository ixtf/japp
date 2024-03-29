package com.github.ixtf;

import com.fasterxml.uuid.Generators;
import com.github.ixtf.codec.Base58;
import lombok.AccessLevel;
import lombok.Cleanup;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.Validate;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.*;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author jzb 2018-08-17
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Jcodec {

    @SneakyThrows(IOException.class)
    public static String sha256Hex(File file) {
        @Cleanup final var fis = new FileInputStream(file);
        return DigestUtils.sha256Hex(fis);
    }

    public static UUID _uuid() {
        return Generators.timeBasedGenerator().generate();
    }

    /**
     * @param names 注意:不要排序，不要 parallel stream，全部原始顺序
     * @return UUID
     */
    public static UUID _uuid(final String... names) {
        final var join = String.join(",", names);
        Validate.notBlank(join);
        final var bytes = join.getBytes(UTF_8);
        return UUID.nameUUIDFromBytes(bytes);
    }

    public static String uuid58(final UUID uuid) {
        final var bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return Base58.encode(bb.array());
    }

    public static String uuid() {
        return _uuid().toString();
    }

    public static String uuid(final String... names) {
        return _uuid(names).toString();
    }

    public static String uuid58() {
        return uuid58(_uuid());
    }

    public static String uuid58(final String... names) {
        return uuid58(_uuid(names));
    }

    public static String password() {
        return password("123456");
    }

    public static String password(final String password) {
        return Optional.ofNullable(password)
                .filter(J::nonBlank)
                .map(__ -> {
                    try {
                        final var salt = new byte[16];
                        SecureRandom.getInstance("SHA1PRNG").nextBytes(salt);
                        final var iterationCount = 30000;
                        final var keySize = 160;
                        final var hash = password(password, salt, iterationCount, keySize);
                        final var baos = new ByteArrayOutputStream();
                        baos.write(255);
                        final var oos = new ObjectOutputStream(baos);
                        oos.write(hash.length);
                        oos.write(salt.length);
                        oos.write(hash);
                        oos.write(salt);
                        oos.writeInt(iterationCount);
                        oos.writeInt(keySize);
                        oos.flush();
                        return Base64.encodeBase64String(baos.toByteArray());
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                })
                .get();
    }

    public static boolean checkPassword(final String encryptPassword, final String password) {
        try {
            Validate.notBlank(encryptPassword);
            final var is = new ByteArrayInputStream(Base64.decodeBase64(encryptPassword));
            final var version = is.read();
            if (version != 255) {
                throw new IOException("version mismatch");
            }
            final var ois = new ObjectInputStream(is);
            final var hash = new byte[ois.read()];
            final var salt = new byte[ois.read()];
            ois.read(hash);
            ois.read(salt);
            final var iterationCount = ois.readInt();
            final var keySize = ois.readInt();
            final var _hash = password(password, salt, iterationCount, keySize);
            return Arrays.equals(hash, _hash);
        } catch (Exception ex) {
            return false;
        }
    }

    private static byte[] password(final String password, final byte[] salt, final int iterationCount, final int keySize) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        Validate.notBlank(password);
        final var spec = new PBEKeySpec(password.toCharArray(), salt, iterationCount, keySize);
        final var factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        return factory.generateSecret(spec).getEncoded();
    }
}

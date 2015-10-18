package moe.minori.pgpclipper.encryption;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

/**
 * Created by Minori on 2015-10-18.
 */
public class PBKDF2Helper {
    // Algorithm Strength Factors

    public static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA1";

    public static final int SALT_LENGTH = 64;
    public static final int HASH_LENGTH = 256;
    public static final int PBKDF2_ITERATIONS = 1000;

    private static byte[] doPBKDF2 (char[] password, byte[] salt, int iterations, int bits)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, bits);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
        return skf.generateSecret(spec).getEncoded();
    }

    public static byte[] makeSalt ()
    {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH/8];
        random.nextBytes(salt);

        return salt;
    }

    public static byte[] createSaltedHash (String input, byte[] salt)
            throws InvalidKeySpecException, NoSuchAlgorithmException {
        char[] inputToCharArray = input.toCharArray();

        return doPBKDF2(inputToCharArray, salt, PBKDF2_ITERATIONS, HASH_LENGTH);
    }

}

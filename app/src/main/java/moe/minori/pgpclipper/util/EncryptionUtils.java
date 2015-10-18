package moe.minori.pgpclipper.util;

/**
 * Created by Minori on 2015-10-18.
 */
public class EncryptionUtils {

    public static String byteArrayToHex (byte[] input)
    {
        if ( input == null )
            return null;

        StringBuilder builder = new StringBuilder();

        for ( byte one : input )
        {
            builder.append(String.format("%02X", one));
        }

        return builder.toString();
    }

    public static byte[] hexToByteArray (String input)
    {
        if ( input == null )
            return null;

        byte [] binary = new byte[input.length() / 2];

        for(int i = 0; i < binary.length; i++)
        {
            binary[i] = (byte) Integer.parseInt(input.substring(2*i, 2*i+2), 16);
        }

        return binary;
    }

    public static byte[] stringToByteArray (String input)
    {
        return input.getBytes();
    }

    public static String byteArrayToString (byte[] input)
    {
        return new String(input);
    }
}

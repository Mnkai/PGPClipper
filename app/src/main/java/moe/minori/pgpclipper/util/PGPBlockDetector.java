package moe.minori.pgpclipper.util;

/**
 * Created by Minori on 2015-09-26.
 */
public class PGPBlockDetector {

    public static final String PGP_SIGNEDMSG_START = "-----BEGIN PGP SIGNED MESSAGE-----";
    public static final String PGP_SIGNEDMSG_END = "-----END PGP SIGNATURE-----";

    public static final String PGP_MESSAGE_START = "-----BEGIN PGP MESSAGE-----";
    public static final String PGP_MESSAGE_END = "-----END PGP MESSAGE-----";

    public static boolean isBlockPresent (String input)
    {
        if ( input != null )
        {
            if ( input.contains(PGP_SIGNEDMSG_START) && input.contains(PGP_SIGNEDMSG_END) ) // signature message with plain text
            {
                return true;
            }
            else if ( input.contains(PGP_MESSAGE_START) && input.contains(PGP_MESSAGE_END) ) // encrypted message (possibly signed)
            {
                return true;
            }
            else // no pgp data
            {
                return false;
            }
        }
        else // text is null
            return false;

    }

    public static String pgpInputTidy (String input)
    {
        String output = null;

        // try to tidy best effort String input
        if ((input.contains(PGPBlockDetector.PGP_SIGNEDMSG_START) && input.contains(PGPBlockDetector.PGP_SIGNEDMSG_END))
                ||
                (input.contains(PGPBlockDetector.PGP_MESSAGE_START) && input.contains(PGPBlockDetector.PGP_MESSAGE_END))
                ) {
            if ( input.contains(PGPBlockDetector.PGP_SIGNEDMSG_START) )
            {
                // tidy string from PGP_SIGNEDMSG_START until PGP_SIGNEDMSG_END
                output = input.substring(
                        input.indexOf(PGPBlockDetector.PGP_SIGNEDMSG_START),
                        input.indexOf(PGPBlockDetector.PGP_SIGNEDMSG_END) + PGPBlockDetector.PGP_SIGNEDMSG_END.length() );
            }
            else if ( input.contains(PGPBlockDetector.PGP_MESSAGE_START) )
            {
                // tidy string from PGP_MESSAGE_START until PGP_MESSAGE_END
                output = input.substring(
                        input.indexOf(PGPBlockDetector.PGP_MESSAGE_START),
                        input.indexOf(PGPBlockDetector.PGP_MESSAGE_END) + PGPBlockDetector.PGP_MESSAGE_END.length() );
            }
        }

        if ( output == null )
            return input;
        else
            return output;
    }
}

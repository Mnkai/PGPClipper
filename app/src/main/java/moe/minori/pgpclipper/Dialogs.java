package moe.minori.pgpclipper;

import android.app.AlertDialog;
import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.widget.EditText;

import moe.minori.pgpclipper.listeners.PINInputReceivedListener;

/**
 * Created by Minori on 2015-10-18.
 */
public class Dialogs {
    public static void showPINDialog (Context c, final PINInputReceivedListener listener)
    {
        final AlertDialog[] dialog = new AlertDialog[1];

        AlertDialog.Builder builder = new AlertDialog.Builder(c);

        builder.setTitle("NFC PIN");
        builder.setMessage("Input PIN");

        final EditText input = new EditText(c);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setTransformationMethod(PasswordTransformationMethod.getInstance());

        final AlertDialog finalDialog = dialog[0];
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 4) {
                    finalDialog.dismiss();

                    listener.onPINReceived(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        builder.setView(input);

        dialog[0] = builder.show();
    }
}

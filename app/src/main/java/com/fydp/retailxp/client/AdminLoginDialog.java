package com.fydp.retailxp.client;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by dmok on 09/03/16.
 */
public class AdminLoginDialog extends DialogFragment {
    public interface AdminLoginDialogListener {
        public void onAdminLoginDialogPositiveClick(DialogFragment dialog);
        public void onAdminLoginDialogNegativeClick(DialogFragment dialog);
    }

    AdminLoginDialogListener mListener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        builder.setMessage(R.string.admin_dialog_text);
        builder.setView(inflater.inflate(R.layout.admin_dialog, null));
        builder.setPositiveButton(R.string.admin_dialog_enter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // process text and enable admin mode
                try {
                    MessageDigest md = MessageDigest.getInstance("MD5");
                    md.digest();
                } catch (NoSuchAlgorithmException e) {
                    System.out.println("NoSuchAlgorithmException: No MD5");
                    e.printStackTrace();
                }
            }
        });
        builder.setNegativeButton(R.string.admin_dialog_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // user cancels the dialog
            }
        });
        return builder.create();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (AdminLoginDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement AdminLoginDialogListener");
        }
    }
}

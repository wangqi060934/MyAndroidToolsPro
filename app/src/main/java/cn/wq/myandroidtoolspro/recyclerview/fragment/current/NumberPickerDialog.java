package cn.wq.myandroidtoolspro.recyclerview.fragment.current;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Toast;

import cn.wq.myandroidtoolspro.R;
import cn.wq.myandroidtoolspro.helper.Utils;

/**
 * Created by wangqi on 2017/7/14.
 */

public class NumberPickerDialog extends DialogFragment {
    private NumberPicker picker;
    private EditText editText;
    public final static int MIN_VALUE = 5;
    public final static int MAX_VALUE = 30;
    public final static int DEFAUT_VALUE = 15;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        final int currentNum = sharedPreferences.getInt("current_fragment_interval", DEFAUT_VALUE * 100) / 100;

        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_num_picker, null);
        if (Build.VERSION.SDK_INT >= 11) {
            picker = (NumberPicker) view.findViewById(R.id.picker);
            picker.setMinValue(MIN_VALUE);
            picker.setMaxValue(MAX_VALUE);
            picker.setValue(currentNum);
        } else {
            editText = (EditText) view.findViewById(R.id.edittext);
            editText.setText(String.valueOf(currentNum));
        }

        return new AlertDialog.Builder(getContext())
                .setView(view)
                .setTitle(R.string.current_fragment_time_title)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int value = currentNum;

                        if (Build.VERSION.SDK_INT >= 11) {
                            value = picker.getValue() * 100;
                        } else {
                            if (!TextUtils.isEmpty(editText.getText())) {
                                try {
                                    value = Integer.parseInt(editText.getText().toString()) * 100;
                                    if (value < MIN_VALUE*100 || value > MAX_VALUE) {
                                        Toast.makeText(getContext(),
                                                "Must between " + MIN_VALUE + " ~ " + MAX_VALUE + " !",
                                                Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                } catch (Exception e) {
                                    return;
                                }
                            }else{
                                return;
                            }
                        }
                        sharedPreferences.edit()
                                .putInt("current_fragment_interval", value)
                                .apply();

                        Intent data = new Intent();
                        data.putExtra("num", value);
                        getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK,data);
                    }
                }).show();
    }
}

package cn.wq.myandroidtoolspro.recyclerview.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import cn.wq.myandroidtoolspro.R;

/**
 * Created by wangqi on 2017/8/18.
 * db single table search condition dialog
 */

public class TableQueryDialogFragment extends DialogFragment implements
        View.OnClickListener {
    private Spinner fields, operations;
    private EditText editText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().setTitle(R.string.query);
        Bundle args = getArguments();

        View rootView = inflater.inflate(R.layout.dialog_table_query,
                container, false);
        fields = (Spinner) rootView.findViewById(R.id.fields);
        ArrayAdapter<String> nameAdapter = new ArrayAdapter<>(
                getActivity(), android.R.layout.simple_spinner_item,
                getArguments().getStringArray("fields"));
        nameAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fields.setAdapter(nameAdapter);
        fields.setSelection(args.getInt("field_pos"));

        operations = (Spinner) rootView.findViewById(R.id.operation);
        ArrayAdapter<String> operationAdapter = new ArrayAdapter<>(
                getActivity(), android.R.layout.simple_spinner_item,
                getResources().getStringArray(R.array.query_operation));
        operationAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        operations.setAdapter(operationAdapter);
        operations.setSelection(args.getInt("operation_pos"));

        editText = (EditText) rootView.findViewById(R.id.value);
        String query_value = args.getString("query_value");
        editText.setText(query_value);
        if (query_value != null) {
            editText.setSelection(query_value.length());
        }

        rootView.findViewById(R.id.cancel).setOnClickListener(this);
        rootView.findViewById(R.id.ok).setOnClickListener(this);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle arg0) {
        super.onActivityCreated(arg0);
        getDialog().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.cancel:
                dismiss();
                break;
            case R.id.ok:
                Intent intent = new Intent();
                intent.putExtra("field_pos", fields.getSelectedItemPosition());
                intent.putExtra("operation_pos",
                        operations.getSelectedItemPosition());
                intent.putExtra("query_value", editText.getText().toString());

                StringBuilder sb = new StringBuilder();
                sb.append(fields.getSelectedItem());
                // operation:contains
                if (operations.getSelectedItemPosition() == 0) {
                    sb.append(" LIKE '%");
                    sb.append(editText.getText().toString());
                    sb.append("%'");
                } else {
                    sb.append(" ");
                    sb.append(operations.getSelectedItem());
                    sb.append(" '");
                    sb.append(editText.getText().toString());
                    sb.append("'");
                }

                intent.putExtra("query_statement", sb.toString());
                getTargetFragment().onActivityResult(getTargetRequestCode(),
                        Activity.RESULT_OK, intent);
                dismiss();
                break;
            default:
                break;
        }
    }

}
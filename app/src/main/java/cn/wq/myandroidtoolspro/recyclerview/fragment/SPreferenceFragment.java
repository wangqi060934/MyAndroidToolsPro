package cn.wq.myandroidtoolspro.recyclerview.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cn.wq.myandroidtoolspro.BuildConfig;
import cn.wq.myandroidtoolspro.R;
import cn.wq.myandroidtoolspro.helper.Utils;
import cn.wq.myandroidtoolspro.recyclerview.toolbar.RecyclerWithToolbarFragment;

public class SPreferenceFragment extends RecyclerWithToolbarFragment {
    private LoadFileTask mTask;
    private SPrefsAdapter mAdapter;
    private int clicked_pos;
    private String full_path;
    private final static int REQUEST_CODE_EDIT_DIALOG = 2;
    private final static int REQUEST_CODE_ADD_DIALOG = 3;

    public static SPreferenceFragment newInstance(Bundle args) {
        SPreferenceFragment fragment = new SPreferenceFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (!BuildConfig.isFree) {
            MenuItem menuItem = menu.add(0, R.id.ok, 0, R.string.add);
            menuItem.setIcon(R.drawable.ic_add_circle_white);
            menuItem.setVisible(false);
            MenuItemCompat.setShowAsAction(menuItem, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (!BuildConfig.isFree) {
            menu.findItem(R.id.ok).setVisible(getRecyclerListView().isShown());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
//		case android.R.id.home:
//            getActivity().onBackPressed();
//			break;
            case R.id.ok:
                AddDialogFragment f = new AddDialogFragment();
                f.setTargetFragment(this, REQUEST_CODE_ADD_DIALOG);
//                f.show(getChildFragmentManager(), "add");
                f.show(getFragmentManager(), "add");
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAdapter = new SPrefsAdapter(getContext());

        setAdapter(mAdapter);

        Bundle data = getArguments();

        initActionbar(1, data.getString("name"));
        setToolbarLogo(data.getString("packageName"));

        if (!Utils.checkSDcard(getContext())) {
            return;
        }

        full_path = data.getString("dir");
        if (!TextUtils.isEmpty(data.getString("name"))) {
            full_path = full_path + "/" + data.getString("name");
        }

        if (mTask == null) {
            mTask = new LoadFileTask();
            mTask.execute(full_path);
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mTask != null) {
            mTask.cancel(true);
        }
    }

    private class LoadFileTask extends
            AsyncTask<String, Void, List<SPrefsEntry>> {
        @Override
        protected List<SPrefsEntry> doInBackground(String... params) {
//            if (Utils.runRootCommand("cp -f " + params[0] + " /sdcard/Android/data/cn.wq.myandroidtoolspro/cache/temp2.xml\n")
//                    || Utils.runRootCommand("cat " + params[0] + " > " + Utils.getTempSPfrefsPath(getActivity()) + "\n")
//                    || Utils.runRootCommand("cat " + params[0] + " > " + System.getenv("EXTERNAL_STORAGE") + "/Android/data/cn.wq.myandroidtoolspro/cache/temp2.xml\n")) {
            final String pkg = getContext().getPackageName();
            if (Utils.runRootCommand("cp -f " + params[0] + " /sdcard/Android/data/"+ pkg +"/cache/temp2.xml\n")
                    || Utils.runRootCommand("cat " + params[0] + " > " + Utils.getTempSPfrefsPath(getContext()) + "\n")
                    || Utils.runRootCommand("cat " + params[0] + " > " + System.getenv("EXTERNAL_STORAGE") + "/Android/data/"+ pkg +"/cache/temp2.xml\n")) {
                try {
                    List<SPrefsEntry> list = new ArrayList<>();
                    XmlPullParser parser = Xml.newPullParser();
                    FileInputStream in = new FileInputStream(Utils.getTempSPfrefsPath(getContext()));
                    parser.setInput(in, "UTF-8");
                    int type;
                    while ((type = parser.next()) != XmlPullParser.END_DOCUMENT) {
                        if (type == XmlPullParser.START_TAG
                                && !"map".equals(parser.getName())) {
                            if ("set".equals(parser.getName())) {
                                list.add(parseStringSet(parser));
                            } else {
                                SPrefsEntry entry = new SPrefsEntry();
                                entry.type = parser.getName();
//                                entry.name = parser.getAttributeValue(0);
                                final int attrCount = parser.getAttributeCount();
                                if (attrCount > 0) {
//                                    entry.value = parser.getAttributeValue(1);
                                    for (int i = 0; i < attrCount; i++) {
                                        if ("name".equals(parser.getAttributeName(i))) {
                                            entry.name = parser.getAttributeValue(i);
                                        } else if ("value".equals(parser.getAttributeName(i))) {
                                            entry.value = parser.getAttributeValue(i);
                                        }
                                    }
                                }
                                if (entry.value == null) {
                                    entry.value = parser.nextText();
                                }

                                list.add(entry);
                            }

                        }
                    }
                    in.close();
                    return list;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return null;
        }

        private SPrefsEntry parseStringSet(XmlPullParser parser) {
            SPrefsEntry entry = new SPrefsEntry();
            entry.name = parser.getAttributeValue(0);
            try {
                StringBuilder sb = new StringBuilder();
                int depth = parser.getDepth();
                int type;
                while ((type = parser.next()) != XmlPullParser.END_DOCUMENT &&
                        (type != XmlPullParser.END_TAG || parser.getDepth() > depth)) {
                    if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                        continue;
                    }
                    sb.append(parser.nextText());
                    sb.append("\n");
                }
                if (sb.length() == 1) {
                    sb.deleteCharAt(0);
                } else if (sb.length() > 1) {
                    sb.deleteCharAt(sb.length() - 1);
                }

                entry.type = "set";
                entry.value = sb.toString();
                return entry;
            } catch (XmlPullParserException | IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<SPrefsEntry> result) {
            super.onPostExecute(result);

            mAdapter.setData(result);

            setListShown(true, isResumed());
            getActivity().supportInvalidateOptionsMenu();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            setListShown(false, true);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_EDIT_DIALOG) {
            switch (resultCode) {
                case Activity.RESULT_OK:
                    mAdapter.updateLine(clicked_pos, data.getStringExtra("value"));
                    saveToFile();
                    break;
                case Activity.RESULT_FIRST_USER:
                    mAdapter.deleteLine(clicked_pos);
                    saveToFile();
                    break;
            }
        } else if (requestCode == REQUEST_CODE_ADD_DIALOG && resultCode == Activity.RESULT_OK) {
            String value = data.getStringExtra("value");
            String name = data.getStringExtra("name");
            String type = data.getStringExtra("type");
            List<SPrefsEntry> list = mAdapter.getList();
            boolean isExists = false;
            for (SPrefsEntry entry : list) {
                if (TextUtils.equals(name, entry.name)) {
                    entry.value = value;
                    entry.type = type;
                    isExists = true;
                    break;
                }
            }
            if (!isExists) {
                SPrefsEntry entry = new SPrefsEntry();
                entry.value = value;
                entry.name = name;
                entry.type = type;
                list.add(entry);
            }
            mAdapter.setData(list);
            saveToFile();
        }

    }

    private void saveToFile() {
        final String ATTRIBUTE_NAME_STRING = "string";
        final String ATTRIBUTE_NAME_MAP = "map";

        XmlSerializer serializer = Xml.newSerializer();
        try {
            FileOutputStream out = new FileOutputStream(Utils.getTempSPfrefsPath(getContext()));
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            serializer.setOutput(out, "utf-8");
            serializer.startDocument("utf-8", true);
            serializer.startTag(null, ATTRIBUTE_NAME_MAP);
            for (SPrefsEntry entry : mAdapter.getList()) {
                serializer.startTag(null, entry.type);
                serializer.attribute(null, "name", entry.name);
                if ("set".equals(entry.type)) {
                    String[] strings = entry.value.split("\n");
                    for (String s : strings) {
                        serializer.startTag(null, ATTRIBUTE_NAME_STRING);
                        serializer.text(s);
                        serializer.endTag(null, ATTRIBUTE_NAME_STRING);
                    }
                } else if ("string".equals(entry.type)) {
                    serializer.startTag(null, ATTRIBUTE_NAME_STRING);
                    serializer.text(entry.value);
                    serializer.endTag(null, ATTRIBUTE_NAME_STRING);
                } else {
                    serializer.attribute(null, "value", entry.value);
                }
                serializer.endTag(null, entry.type);
            }

            serializer.endTag(null, ATTRIBUTE_NAME_MAP);
            serializer.endDocument();
            out.close();
        } catch (IllegalArgumentException | IllegalStateException | IOException e) {
            e.printStackTrace();
        }

        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... arg0) {
//                return Utils.runRootCommand("cat /sdcard/Android/data/cn.wq.myandroidtoolspro/cache/temp2.xml > " + full_path + "\n")
//                        || Utils.runRootCommand("cat " + Utils.getTempSPfrefsPath(getActivity()) + " > " + full_path + "\n")
//                        || Utils.runRootCommand("cat " + System.getenv("EXTERNAL_STORAGE") + "/Android/data/cn.wq.myandroidtoolspro/cache/temp2.xml > " + full_path + "\n");
                final String pkg = getContext().getPackageName();
                return Utils.runRootCommand("cat /sdcard/Android/data/"+ pkg +"/cache/temp2.xml > " + full_path + "\n")
                        || Utils.runRootCommand("cat " + Utils.getTempSPfrefsPath(getContext()) + " > " + full_path + "\n")
                        || Utils.runRootCommand("cat " + System.getenv("EXTERNAL_STORAGE") + "/Android/data/"+ pkg +"/cache/temp2.xml > " + full_path + "\n");
            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);
                if (!result) {
                    Toast.makeText(getContext(), "Save Failed", Toast.LENGTH_SHORT).show();
                }
            }

        }.execute();
    }


    public static class EditDialogFragment extends DialogFragment implements OnClickListener {
        private EditText editText;
        private RadioGroup radioGroup;
        private RadioButton trueRadioButton, falseRadioButton;
        /**
         * default:string 1:int 2:long 3:float 4:boolean
         */
        private int valueType;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            getDialog().setTitle(R.string.edit);
            View rootView = inflater.inflate(R.layout.dialog_spref_edit, container, false);

            Bundle args = getArguments();
            String typeString = args.getString("type");
            String mValue = args.getString("value");

            TextView type = (TextView) rootView.findViewById(R.id.type);
            type.setText(getString(R.string.format_type,typeString));

            TextView name = (TextView) rootView.findViewById(R.id.name);
            name.setText(getString(R.string.format_name,args.getString("name")));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                name.setTextIsSelectable(true);
            }

            editText = (EditText) rootView.findViewById(R.id.value);
            radioGroup = (RadioGroup) rootView.findViewById(R.id.boolChoose);
            if ("int".equals(typeString)) {
                valueType = 1;
            } else if ("long".equals(typeString)) {
                valueType = 2;
            } else if ("float".equals(typeString)) {
                valueType = 3;
            } else if ("boolean".equals(typeString)) {
                valueType = 4;
            }

            if (BuildConfig.isFree) {
                switch (valueType) {
                    case 4:
                        editText.setVisibility(View.INVISIBLE);
                        radioGroup.setVisibility(View.VISIBLE);

                        trueRadioButton = (RadioButton) rootView.findViewById(R.id.trueValue);
                        falseRadioButton = (RadioButton) rootView.findViewById(R.id.falseValue);
                        if ("true".equals(mValue)) {
                            trueRadioButton.setChecked(true);
                        } else {
                            falseRadioButton.setChecked(true);
                        }
                        radioGroup.setEnabled(false);
                        break;
                    default:
                        editText.setText(mValue);
                        editText.setKeyListener(null);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                            editText.setTextIsSelectable(true);
                        }
                        break;
                }

                rootView.findViewById(R.id.ok).setVisibility(View.GONE);
                rootView.findViewById(R.id.delete).setVisibility(View.GONE);
            }else{
                getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

                switch (valueType) {
                    case 1:
                    case 2:
                        editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
                        editText.setText(mValue);
                        break;
                    case 3:
                        editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                        editText.setText(mValue);
                        break;
                    case 4:
                        editText.setVisibility(View.INVISIBLE);
                        radioGroup.setVisibility(View.VISIBLE);

                        trueRadioButton = (RadioButton) rootView.findViewById(R.id.trueValue);
                        falseRadioButton = (RadioButton) rootView.findViewById(R.id.falseValue);
                        if ("true".equals(mValue)) {
                            trueRadioButton.setChecked(true);
                        } else {
                            falseRadioButton.setChecked(true);
                        }
                        break;
                    default:
                        editText.setText(mValue);
                        editText.setSelection(editText.length());
                        break;
                }

                rootView.findViewById(R.id.ok).setOnClickListener(this);
                rootView.findViewById(R.id.delete).setOnClickListener(this);
            }

            rootView.findViewById(R.id.cancel).setOnClickListener(this);
            return rootView;
        }

        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.cancel:
                    dismiss();
                    break;
                case R.id.delete:
                    getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_FIRST_USER, null);
                    dismiss();
                    break;
                case R.id.ok:
                    switch (valueType) {
                        case 1:
                            String valueString = editText.getText().toString().trim();
                            try {
                                Integer.parseInt(valueString);
                                setTheResult(valueString);
                            } catch (NumberFormatException e) {
                                editText.setError(getString(R.string.input_error_int));
                                hideInputMethod();
                            }
                            break;
                        case 2:
                            valueString = editText.getText().toString().trim();
                            try {
                                Long.parseLong(valueString);
                                setTheResult(valueString);
                            } catch (NumberFormatException e) {
                                editText.setError(getString(R.string.input_error_long));
                                hideInputMethod();
                            }
                            break;
                        case 3:
                            valueString = editText.getText().toString().trim();
                            try {
                                Float.parseFloat(valueString);
                                setTheResult(valueString);
                            } catch (NumberFormatException e) {
                                editText.setError(getString(R.string.input_error_float));
                                hideInputMethod();
                            }
                            break;
                        case 4:
                            setTheResult(trueRadioButton.isChecked() ? "true" : "false");
                            break;
                        default:
                            setTheResult(editText.getText().toString());
                            break;
                    }

                    break;
                default:
                    break;
            }
        }

        private void setTheResult(String value) {
            Intent data = new Intent();
            data.putExtra("value", value);
            getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, data);
            dismiss();
        }

        private void hideInputMethod() {
            InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(editText.getWindowToken(), 0);
        }
    }

    public static class AddDialogFragment extends DialogFragment {
        private final String[] typeValues = new String[]{"string", "int", "long", "float", "boolean"};

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            View rootView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_spref_add, null, false);
            final Spinner spinner = (Spinner) rootView.findViewById(R.id.type);
            final EditText nameEtv = (EditText) rootView.findViewById(R.id.name);
            final EditText valueEtv = (EditText) rootView.findViewById(R.id.value);
            final RadioGroup radioGroup = (RadioGroup) rootView.findViewById(R.id.boolChoose);
            final RadioButton trueRadioButton = (RadioButton) rootView.findViewById(R.id.trueValue);
            final RadioButton falseRadioButton = (RadioButton) rootView.findViewById(R.id.falseValue);

            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, typeValues);
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(arrayAdapter);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    radioGroup.setVisibility(position == 4 ? View.VISIBLE : View.INVISIBLE);
                    valueEtv.setVisibility(position == 4 ? View.INVISIBLE : View.VISIBLE);
                    valueEtv.getText().clear();
                    switch (position) {
                        case 1:
                        case 2:
                            valueEtv.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
                            break;
                        case 3:
                            valueEtv.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                            break;
                        case 4:
                            falseRadioButton.setChecked(true);
                            break;
                        default:
                            break;
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

            return new AlertDialog.Builder(getContext()).setTitle(R.string.add)
                    .setView(rootView)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            int spinnerPos = spinner.getSelectedItemPosition();
                            Intent data = new Intent();
                            data.putExtra("type", typeValues[spinnerPos]);
                            data.putExtra("name", nameEtv.getText().toString());
                            data.putExtra("value", spinnerPos == 4 ? (trueRadioButton.isChecked() ? "true" : "false") : valueEtv.getText().toString());
                            getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, data);
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .create();
        }

    }

    private class SPrefsEntry {
        String type;
        String name;
        String value;
    }

    private class SPrefsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private Context mContext;
        private List<SPrefsEntry> mList;
        private static final int TYPE_HEADER = 0;
        private static final int TYPE_LIST = 1;

        SPrefsAdapter(Context mContext) {
            super();
            this.mContext = mContext;
            this.mList = new ArrayList<>();
        }

        public void setData(List<SPrefsEntry> list) {
            mList.clear();
            if (list != null) {
                mList.addAll(list);
            }
            notifyDataSetChanged();
        }

        public List<SPrefsEntry> getList() {
            return new ArrayList<>(mList);
        }

        void updateLine(int pos, String newValue) {
            SPrefsEntry entry = mList.get(pos);
            entry.value = newValue;
            mList.set(pos, entry);
            notifyItemChanged(pos + 1);   //需要加1个 header
        }

        void deleteLine(int pos) {
            mList.remove(pos);
            notifyItemRemoved(pos + 1);
        }

        public Object getItem(int arg0) {
            return mList.get(arg0);
        }

        @Override
        public int getItemViewType(int position) {
            return position == 0 ? TYPE_HEADER : TYPE_LIST;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == TYPE_HEADER) {
                return new HeaderHolder(LayoutInflater.from(getContext())
                        .inflate(R.layout.item_sharedpref_row, parent, false));
            } else {
                return new VHolder(LayoutInflater.from(mContext)
                        .inflate(R.layout.item_sharedpref_row, parent, false));
            }

        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (getItemViewType(position) == TYPE_LIST) {
                VHolder vHolder = (VHolder) holder;
                SPrefsEntry entry = mList.get(position - 1);
                vHolder.type.setText(entry.type);
                vHolder.name.setText(entry.name);
                vHolder.value.setText(entry.value);
            }
        }

        @Override
        public int getItemCount() {
            return mList.size() + 1;
        }

    }

    private class HeaderHolder extends RecyclerView.ViewHolder {
        public HeaderHolder(View itemView) {
            super(itemView);
            itemView.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.actionbar_color_light));

            TextView type = (TextView) itemView.findViewById(R.id.type);
            TextView name = (TextView) itemView.findViewById(R.id.name);
            TextView value = (TextView) itemView.findViewById(R.id.value);

            type.setText(R.string.type);
            name.setText(R.string.name);
            value.setText(R.string.value);
        }
    }


    private class VHolder extends RecyclerView.ViewHolder {
        TextView type, name, value;

        public VHolder(View itemView) {
            super(itemView);
            type = (TextView) itemView.findViewById(R.id.type);
            name = (TextView) itemView.findViewById(R.id.name);
            value = (TextView) itemView.findViewById(R.id.value);

            itemView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getLayoutPosition();
                    if (position == 0) {
                        return;
                    }
                    clicked_pos = position - 1;
                    SPrefsEntry entry = (SPrefsEntry) mAdapter.getItem(clicked_pos);

                    EditDialogFragment dialog = new EditDialogFragment();
                    dialog.setTargetFragment(SPreferenceFragment.this, REQUEST_CODE_EDIT_DIALOG);
                    Bundle args = new Bundle();
                    args.putString("type", entry.type);
                    args.putString("name", entry.name);
                    args.putString("value", entry.value);

                    dialog.setArguments(args);
//                    dialog.show(getChildFragmentManager(), "sp");
                    dialog.show(getFragmentManager(), "sp");
                }
            });


        }
    }

}

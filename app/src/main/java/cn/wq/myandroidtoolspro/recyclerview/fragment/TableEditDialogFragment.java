package cn.wq.myandroidtoolspro.recyclerview.fragment;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Base64;
import android.util.SparseBooleanArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import cn.wq.myandroidtoolspro.BuildConfig;
import cn.wq.myandroidtoolspro.R;
import cn.wq.myandroidtoolspro.helper.Utils;

/**
 * Created by wangqi on 2017/8/18.
 */

public class TableEditDialogFragment extends DialogFragment implements
        View.OnClickListener {
    private String table_name;
    private String[] titles;
    private SparseBooleanArray isPrimaryKey;
//		private SparseBooleanArray isBlob;
    private String query_statement;
    private int row;
    private int page;
    //        private final String[] typesConstant=new String[]{"integer","numeric","real","blob","text"};
    private String[] types;
    private boolean isAddMode;
    private boolean isCloneMode;
    private ArrayList<String> valueList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Bundle args = getArguments();
//			titles = args.getStringArray("titles");
        table_name = args.getString("table_name");
        row = args.getInt("row", -1);
        page = args.getInt("page", 1);
        isCloneMode = args.getBoolean("isClone", false);
        if (row < 0) {  //新增模式
            isAddMode = true;
            getDialog().setTitle(R.string.add);
        } else {
            if (isCloneMode) { //clone模式
                getDialog().setTitle(R.string.clone_record);
            }else{    //edit模式
                getDialog().setTitle("Row:#" + row);
            }
            query_statement = args.getString("query_statement");
        }

        valueList = args.getStringArrayList("value");

        isPrimaryKey = new SparseBooleanArray();
//			isBlob = new SparseBooleanArray();

        SQLiteDatabase db = SQLiteDatabase.openDatabase(Utils.getTempDBPath(getContext()),
                null, 0);
        Cursor typeCursor = db.rawQuery("PRAGMA table_info(" + table_name + ")", null);
        types = new String[typeCursor.getCount()];
        titles = new String[typeCursor.getCount()];
        int index = 0;
        while (typeCursor.moveToNext()) {
            if(typeCursor.getInt(typeCursor.getColumnIndex("pk"))>0){
//                isPrimaryKey.put(typeCursor.getPosition(), true);
                isPrimaryKey.put(index, true);
            }
            types[index] = typeCursor.getString(typeCursor.getColumnIndex("type"));
            titles[index++] = typeCursor.getString(typeCursor.getColumnIndex("name"));
//                if (typeCursor.getString(typeCursor.getColumnIndex("type"))
//						.equals("blob")) {
//					isBlob.put(typeCursor.getPosition(), true);
//				}
        }
        typeCursor.close();
        db.close();

        View rootView = inflater.inflate(R.layout.dialog_table_edit, null);
        rootView.findViewById(R.id.cancel).setOnClickListener(this);
        if (BuildConfig.isFree) {
            rootView.findViewById(R.id.ok).setVisibility(View.GONE);
        }else{
            getDialog().getWindow().setSoftInputMode(   //pro默认显示键盘
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            rootView.findViewById(R.id.ok).setOnClickListener(this);
        }

        LinearLayout content = (LinearLayout) rootView
                .findViewById(R.id.content);
        content.setPadding(15, 15, 15, 15);
        final int size = titles.length;
        for (int i = 0; i < size; i++) {
            TextView t = new TextView(getContext());
            t.setText(titles[i] + ":");
            content.addView(t);
            final boolean isBlob = "blob".equalsIgnoreCase(types[i]);
            if (isBlob) {
                if (isAddMode) {
                    TextView tv = new TextView(getContext());
                    tv.setText("   **ignoe blob type**");
                    tv.setId(i);
                    content.addView(tv);
                } else {
                    boolean isImage = false;
                    String encodeStr = getValue(i);

                    if (!TextUtils.isEmpty(encodeStr) && encodeStr.length() > 8) {
                        byte[] bytes = Base64.decode(encodeStr, Base64.DEFAULT);

                        final int len = bytes.length;
                        //图片的magic number: http://www.garykessler.net/library/file_sigs.html
                        //1. jpeg图片以 ffd8 开头，ffd9结尾
//                        if("ffd8".equals(String.format("%02x%02x",bytes[0],bytes[1]))
//                                &&"ffd9".equals(String.format("%02x%02x",bytes[len-2],bytes[len-1]))){
                        //2. png图片以 89 50 4E 47 0D 0A 1A 0A 开头（8个字节）
                        if ((bytes[0] == -1 && bytes[1] == -40 && bytes[len - 2] == -1 && bytes[len - 1] == -39)
                                || (bytes[0] == -119 && bytes[1] == 80 && bytes[2] == 78 && bytes[3] == 71 && bytes[4] == 13 && bytes[5] == 10 && bytes[6] == 26 && bytes[7] == 10)) {
                            isImage = true;
                            ImageView imageView = new ImageView(getContext());
                            imageView.setId(i);
                            imageView.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
                            content.addView(imageView);
                        }
                    }
                    if (!isImage) {
                        TextView tv = new TextView(getContext());
                        tv.setId(i);
                        tv.setText(TextUtils.isEmpty(encodeStr) ? "" : "   *(BLOB data)*");
                        content.addView(tv);
                    }
                }
            } else {
                EditText v = new EditText(getContext());
                v.setId(i);
                v.setTextSize(18f);
                if (!isAddMode) {
                    v.setText(getValue(i));
                }
                if (BuildConfig.isFree) {
//                    v.setEnabled(false);  //效果不好看
                    v.setKeyListener(null);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        v.setTextIsSelectable(true);
                    }
                }else{
                    setInputType(types[i], v);
                }


                //不能修改
//                    if (isBlob) {
//                        v.setKeyListener(null);
//                    }
                content.addView(v);
            }
        }

        return rootView;
    }

    private String getValue(int index) {
        if (valueList == null || index < 0 || index >= valueList.size()) {
            return "";
        }
        return valueList.get(index);
    }

//    private String bytesToHex(byte[] in) {
//        if (in == null || in.length == 0) {
//            return "";
//        }
//        final StringBuilder builder = new StringBuilder();
//        for(byte b : in) {
//            builder.append(String.format("%02x", b));
//        }
//        return builder.toString();
//    }

    //8位hex转int,d8 -> -40
//    private int eightBitsHexToInt(int hex){
//        boolean isNeg = ((hex>>>7)&1) ==1; //最高位是否是1
//        if(!isNeg){
//            return hex;
//        }else{	//低位取反+1 反过来操作
//            int lowBits=hex & 0x7f;
//            int allBits = (lowBits-1) & 0xff;
//            allBits = ~allBits;
//            allBits = allBits &(0x7f);
//            return -allBits;
//        }
//    }


    private void setInputType(String type, EditText editText) {
        switch (type.toLowerCase()) {
            case "integer":
            case "numeric":
                editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
                break;
            case "real":
                editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                break;
            default:
                break;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ok:
                if (getView() == null) {
                    dismiss();
                    return;
                }
                ArrayList<String> values = new ArrayList<>();
                SQLiteDatabase db = SQLiteDatabase.openDatabase(
                        Utils.getTempDBPath(getContext()), null, 0);
                ContentValues cv = new ContentValues();
                final int size = titles.length;
                for (int i = 0; i < size; i++) {
                    View  v = getView().findViewById(i);
                    if (v == null) {
                        continue;
                    }
                    //修改模式 忽略主键
                    boolean isIgnoreToChange = !isAddMode && !isCloneMode && isPrimaryKey.get(i);

                    if (v instanceof EditText) {
                        EditText editText = (EditText) v;
                        values.add(editText.getText().toString());
                        if (!isIgnoreToChange && !"blob".equalsIgnoreCase(types[i])) {
                            cv.put(titles[i], editText.getText().toString());
                        }
                    } else if (v instanceof TextView ||v instanceof ImageView) { //blob（图片）
                        ArrayList<String> value = getArguments().getStringArrayList("value");
                        String encodeStr = value.get(i);
                        values.add(encodeStr);
                        if (!isIgnoreToChange) {
                            if (!TextUtils.isEmpty(encodeStr)) {
                                byte[] bytes = Base64.decode(encodeStr, Base64.DEFAULT);
                                cv.put(titles[i], bytes);
                            }else{
                                cv.putNull(titles[i]);
                            }
                        }
                    }

                }

                try {
                    if (isAddMode || isCloneMode) {
//                            db.insert(table_name, null, cv);
                        db.insertOrThrow(table_name, null, cv);
                        getTargetFragment().onActivityResult(
                                getTargetRequestCode(), Activity.RESULT_OK, null);
                        dismiss();
                    } else {
                        StringBuilder sb = new StringBuilder();
                        sb.append("rowid = (select rowid from ");
                        sb.append(table_name);
                        if (query_statement != null) {
                            sb.append(" where ");
                            sb.append(query_statement);
                        }
                        sb.append(" limit 1 offset ");
//                        sb.append(row - 1);
                        sb.append((page - 1) * TableSingleFragment.NUM_PER_PAGE + row - 1);
                        sb.append(")");
                        // fixme: 2018/8/16 isPrimaryKey已排除了主键，可能还需要提示unique字段
                        db.update(table_name, cv, sb.toString(), null);
                        Intent data = new Intent();
                        data.putStringArrayListExtra("values", values);
                        data.putExtra("row", row);
                        getTargetFragment().onActivityResult(
                                getTargetRequestCode(), Activity.RESULT_OK, data);
                        dismiss();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast toast = Toast.makeText(getContext(), e.getMessage(),
                            Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
                db.close();

                break;
            case R.id.cancel:
                dismiss();
                break;
            default:
                break;
        }
    }

}
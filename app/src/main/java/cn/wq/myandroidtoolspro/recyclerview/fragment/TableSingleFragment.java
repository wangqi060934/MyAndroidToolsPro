package cn.wq.myandroidtoolspro.recyclerview.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Base64;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

import cn.wq.myandroidtoolspro.BuildConfig;
import cn.wq.myandroidtoolspro.R;
import cn.wq.myandroidtoolspro.helper.Utils;
import cn.wq.myandroidtoolspro.recyclerview.toolbar.BaseFragment;
import cn.wq.myandroidtoolspro.views.DataGrid;

/**
 * Created by wangqi on 2017/8/18.
 */

public  class TableSingleFragment extends BaseFragment {
    private LoadDataTask mTask;
    private View progressContainer, dataGridParent, operationParent;
    private DataGrid dataGrid;
    private EditText pageEditText;
    private TextView totalPageTv;
    private ImageButton previousBtn, nextBtn, firstBtn, lastBtn;
    private String[] table_fields;
    private static final int REQUEST_CODE_QUERY = 0;
    private static final int REQUEST_CODE_EDIT = 1;
    private static final int REQUEST_CODE_ADD = 2;
    private static final int REQUEST_CODE_DELETE_CUR_PAGE = 3;
    private String table_name, query_value, query_statement;
    private int field_pos, operation_pos;
    private String fullPath;
    //0是title，1开始才是数据
    private int selected_row;
    private int currentPage = 1, totalPage;
    public static final int NUM_PER_PAGE = 500;
    private Context mContext;
    private boolean hasWal;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    public static TableSingleFragment newInstance(Bundle args) {
        TableSingleFragment fragment = new TableSingleFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Bundle args = getArguments();
        table_name = args.getString("table");
        fullPath = args.getString("fullPath");
        hasWal = args.getBoolean("hasWal");

        initActionbar(1,table_name);

        //table_name如果中间有.点号需要用`或者双引号包裹起来
        table_name = "`" + table_name + "`";
        setToolbarLogo(args.getString("packageName"));

        if (mTask == null) {
            mTask = new LoadDataTask();
            mTask.execute(new String[]{null});
        }
        pageEditText.setSelection(pageEditText.getText().length());
        pageEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    if (TextUtils.isEmpty(v.getText())) {
                        currentPage = 1;
                    } else {
                        try {
                            currentPage = Integer.parseInt(v.getText().toString());
                            if (currentPage < 1) {
                                currentPage = 1;
                            }
                        } catch (NumberFormatException e) {
                            currentPage = 1;
                        }
                    }

                    InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputMethodManager.hideSoftInputFromWindow(pageEditText.getWindowToken(), 0);

                    mTask = new LoadDataTask();
                    mTask.execute(query_statement);
                    return true;
                }
                return false;
            }
        });
        previousBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentPage -= 1;
                if (currentPage < 1) {
                    currentPage = 1;
                }
                mTask = new LoadDataTask();
                mTask.execute(query_statement);
            }
        });
        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentPage += 1;
                mTask = new LoadDataTask();
                mTask.execute(query_statement);
            }
        });
        firstBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentPage = 1;
                mTask = new LoadDataTask();
                mTask.execute(query_statement);
            }
        });
        lastBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentPage = totalPage;
                mTask = new LoadDataTask();
                mTask.execute(query_statement);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mTask != null) {
            mTask.cancel(true);
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.datagrid_with_toolbar_fragment,
                container, false);
        progressContainer = rootView.findViewById(R.id.progressContainer);
        dataGrid = (DataGrid) rootView.findViewById(R.id.datagrid);
        dataGridParent = rootView.findViewById(R.id.datagrid_parent);
        operationParent = rootView.findViewById(R.id.operation_parent);
        pageEditText = (EditText) rootView.findViewById(R.id.page);
        previousBtn = (ImageButton) rootView.findViewById(R.id.page_previous);
        nextBtn = (ImageButton) rootView.findViewById(R.id.page_next);
        firstBtn = (ImageButton) rootView.findViewById(R.id.page_first);
        lastBtn = (ImageButton) rootView.findViewById(R.id.page_last);
        totalPageTv = (TextView) rootView.findViewById(R.id.total_page);
        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        if (requestCode == REQUEST_CODE_QUERY) {
            field_pos = data.getIntExtra("field_pos", 0);
            operation_pos = data.getIntExtra("operation_pos", 0);
            query_value = data.getStringExtra("query_value");

            mTask = new LoadDataTask();
            query_statement = data.getStringExtra("query_statement");
            mTask.execute(query_statement);
        } else if (requestCode == REQUEST_CODE_EDIT) {
            ArrayList<String> values = data
                    .getStringArrayListExtra("values");
            int row = data.getIntExtra("row", 0);
            dataGrid.updateLine(row, values);
            saveToSourceData();
        } else if (requestCode == REQUEST_CODE_ADD) {
            mTask = new LoadDataTask();
            mTask.execute(new String[]{null});
            saveToSourceData();
        } else if (requestCode == REQUEST_CODE_DELETE_CUR_PAGE) {
            //参考 R.id.delete 点击事件；注意分页处理
            if (selected_row >= 0) {
                //清除选中状态，menu 更新
                selected_row = -1;
//                getActivity().supportInvalidateOptionsMenu();
                getActivity().invalidateOptionsMenu();
            }

            StringBuilder sb = new StringBuilder();

            sb.append("rowid in (select rowid from ");
            sb.append(table_name);
            if (query_statement != null) {
                sb.append(" where ");
                sb.append(query_statement);
            }
            sb.append(" limit ").append((currentPage - 1) * NUM_PER_PAGE).append(",").append(NUM_PER_PAGE);
            sb.append(");");

            SQLiteDatabase db = SQLiteDatabase.openDatabase(Utils.getTempDBPath(getContext()), null, 0);
            db.delete(table_name, sb.toString(), null);
            db.close();

            saveToSourceData();
            Toast.makeText(getContext(), "当前页面数据删除成功", Toast.LENGTH_SHORT).show();

            mTask = new LoadDataTask();
            mTask.execute(query_statement);
        }
    }


    private class LoadDataTask extends
            AsyncTask<String, Void, SparseArray<ArrayList<String>>> {
        private ArrayList<Integer> widthList = new ArrayList<>();

        @Override
        protected SparseArray<ArrayList<String>> doInBackground(
                String... params) {
            float density = getResources().getDisplayMetrics().density;
            Paint paint = new Paint();
            paint.setTextSize(14 * density);
            int textPadding = (int) (10 * density);
            int maxCellWidth = (int) (200 * density);
            SparseBooleanArray biggerThanMax = new SparseBooleanArray();

            SQLiteDatabase db = SQLiteDatabase.openDatabase(
                    Utils.getTempDBPath(mContext), null, 0);

            StringBuilder totalSql = new StringBuilder("SELECT COUNT(*) FROM ")
                    .append(table_name);
            if (!TextUtils.isEmpty(query_statement)) {
                totalSql.append(" WHERE ").append(query_statement);
            }
            Cursor totalCursor = db.rawQuery(totalSql.toString(), null);
            if (totalCursor != null && totalCursor.moveToFirst()) {
                int total = totalCursor.getInt(0);
                totalPage = (int) Math.ceil(total / (float) NUM_PER_PAGE);
                totalCursor.close();
            } else {
                totalPage = 0;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("SELECT * FROM ");
            sb.append(table_name);
            if (params[0] != null) {
                sb.append(" WHERE ");
                sb.append(params[0]);
            }
            sb.append(" LIMIT ").append((currentPage - 1) * NUM_PER_PAGE).append(",").append(NUM_PER_PAGE);

            SparseArray<ArrayList<String>> maps = new SparseArray<>();
            Cursor cursor = db.rawQuery(sb.toString(), null);
            if (cursor != null) {
                String[] names = cursor.getColumnNames();
                table_fields = names;
                final int length = names.length;

                ArrayList<String> titles = new ArrayList<>();
                for (int i = 0; i < length; i++) {
                    titles.add(names[i]);
                    int measuredTitleWidth = (int) (paint
                            .measureText(names[i])) + textPadding * 2;
                    if (measuredTitleWidth > maxCellWidth) {
                        biggerThanMax.put(i, true);
                    }
                    widthList.add(measuredTitleWidth);
                }
                maps.put(0, titles);

                int num = 0;
                final int column = cursor.getColumnCount();
                while (cursor.moveToNext()) {
                    num++;
                    ArrayList<String> values = new ArrayList<>();
                    for (int i = 0; i < column; i++) {
                        String value;
                        if (cursor.isNull(i)) {
                            value = "";
                        } else {
                            try {
                                value = cursor.getString(i);
                            } catch (SQLiteException e) {
                                byte[] bytes = cursor.getBlob(i);
//                                    value = "BLOB[size="
//                                            + (bytes == null ? 0
//                                            : bytes.length) + "]";



                                if (bytes == null) {
                                    value = "";
                                }else{
                                    //把byte array转成String，后面想转回byte array其实无法转回同样的array
//                                        value = new String(bytes, Charset.forName("utf-16"));
                                    //用base64转城string
                                    value = Base64.encodeToString(bytes, Base64.DEFAULT);
                                }
                            }
                        }

                        values.add(value);

                        final int index = value.indexOf("\n");
                        String measureValue;
                        if (index != -1) {
                            measureValue = value.substring(0, index);
                        } else {
                            measureValue = value;
                        }
                        //只测量前100个的宽度
                        if (num <= 50) {
                            final int measureWidth = (int) (paint
                                    .measureText(measureValue))
                                    + textPadding * 2;
                            if (widthList.get(i) < measureWidth) {
                                widthList.set(i, measureWidth);
                            }
                        }
                    }
                    maps.put(num, values);
                }

                for (int i = 0; i < column; i++) {
                    if (!biggerThanMax.get(i)
                            && widthList.get(i) > maxCellWidth) {
                        widthList.set(i, maxCellWidth);
                    }
                    widthList.set(i,
                            (i == 0 ? 0 : widthList.get(i - 1))
                                    + widthList.get(i));
                }
                cursor.close();
            }
            db.close();

            return maps;
        }

        private String bytesToHex(byte[] in) {
            if (in == null || in.length == 0) {
                return "";
            }
            final StringBuilder builder = new StringBuilder();
            for(byte b : in) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        }

        @Override
        protected void onPostExecute(SparseArray<ArrayList<String>> result) {
            super.onPostExecute(result);
            dataGridParent.setVisibility(View.VISIBLE);
            progressContainer.setVisibility(View.INVISIBLE);

            if (totalPage > 1) {
                totalPageTv.setText("/" + totalPage);
                pageEditText.setText("" + currentPage);
                pageEditText.setSelection(pageEditText.getText().length());
                operationParent.setVisibility(View.VISIBLE);
            } else {
                operationParent.setVisibility(View.GONE);
            }

            dataGrid.setData(result, widthList);
            dataGrid.setOnLongPressListener(new DataGrid.OnLongPressListener() {
                @Override
                public void onLongPress(int selectedRow,
                                        // ArrayList<String> title,
                                        ArrayList<String> value) {
//                    TableEditDialogFragment dialog = new TableEditDialogFragment();
//                    dialog.setTargetFragment(TableSingleFragment.this,
//                            REQUEST_CODE_EDIT);
//
//                    Bundle args = new Bundle();
//                    // args.putStringArrayList("title", title);
//                    args.putStringArrayList("value", value);
//                    args.putString("table_name", table_name);
////                    args.putStringArray("titles", table_fields);
//                    args.putInt("row", selectedRow);
//                    args.putString("query_statement", query_statement);
//                    dialog.setArguments(args);
//                    dialog.show(getChildFragmentManager(), "edit");

                    showEditDialog(selectedRow,value);
                }
            });

            dataGrid.setOnSelectChangedListener(new DataGrid.OnSelectChangedListener() {
                @Override
                public void onSelectChanged(int selectedRow,
                                            ArrayList<String> value) {
                    selected_row = selectedRow;
                    getActivity().supportInvalidateOptionsMenu();
                }
            });
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dataGridParent.setVisibility(View.INVISIBLE);
            progressContainer.setVisibility(View.VISIBLE);
        }

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.table, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (!BuildConfig.isFree) {
            menu.findItem(R.id.edit).setVisible(selected_row > 0);
            menu.findItem(R.id.delete).setVisible(selected_row > 0);
            menu.findItem(R.id.clone).setVisible(selected_row > 0);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.create_table_statement) {
            StringBuilder sb = new StringBuilder("SELECT sql FROM sqlite_master WHERE type='table' AND name= ");
            DatabaseUtils.appendEscapedSQLString(sb, table_name);
            SQLiteDatabase db = SQLiteDatabase.openDatabase(Utils.getTempDBPath(getContext()),
                    null, 0);
//                    Cursor cursor = db.query("sqlite_master", new String[]{"sql"}, "name=?", new String[]{table_name}, null, null, null);
            Cursor cursor = db.rawQuery(sb.toString(), null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    TextView tv = new TextView(mContext);
                    tv.setTextSize(14);
                    tv.setPadding(50,10,50,10);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        tv.setTextIsSelectable(true);
                    }
                    tv.setText(cursor.getString(0));
                    new AlertDialog.Builder(getContext())
                            .setTitle(R.string.create_table_statement)
//                            .setMessage(cursor.getString(0))
                            .setView(tv)
                            .setPositiveButton(R.string.ok, null)
                            .show();
                }
                cursor.close();
            }
            db.close();
        }else if(item.getItemId() == R.id.table_search){
            TableQueryDialogFragment query = new TableQueryDialogFragment();
            query.setTargetFragment(this, REQUEST_CODE_QUERY);

            Bundle args = new Bundle();
            args.putStringArray("fields", table_fields);
            args.putInt("field_pos", field_pos);
            args.putInt("operation_pos", operation_pos);
            args.putString("query_value", query_value);

            query.setArguments(args);
//            query.show(getChildFragmentManager(), "query");
            query.show(getFragmentManager(), "query");
        }

//        if (!BuildConfig.isFree) {
            switch (item.getItemId()) {
                case R.id.edit: {
                    if (selected_row < 0) {
                        break;
                    }

                    showEditDialog(selected_row,dataGrid.getData().get(selected_row));
                }
                break;
                case R.id.delete: {
                    if (selected_row < 0) {
                        break;
                    }
                    StringBuilder sb = new StringBuilder();

                    sb.append("rowid = (select rowid from ");
                    sb.append(table_name);
                    if (query_statement != null) {
                        sb.append(" where ");
                        sb.append(query_statement);
                    }
                    sb.append(" limit 1 offset ");
//                    sb.append(selected_row - 1);
                    sb.append((currentPage - 1) * NUM_PER_PAGE + selected_row -1);
                    sb.append(");");

                    SQLiteDatabase db = SQLiteDatabase.openDatabase(Utils.getTempDBPath(getContext()), null, 0);
                    db.delete(table_name, sb.toString(), null);
                    db.close();

                    saveToSourceData();
                    dataGrid.deleteLine(selected_row);
                    //清除选中状态，menu 更新
                    selected_row = -1;
//                    getActivity().supportInvalidateOptionsMenu();
                    getActivity().invalidateOptionsMenu();
                }
                break;
                case R.id.add: {
                    //integer,text,blob,real,numeric
                    TableEditDialogFragment dialog = new TableEditDialogFragment();
                    dialog.setTargetFragment(TableSingleFragment.this,
                            REQUEST_CODE_ADD);
                    Bundle args = new Bundle();
                    args.putString("table_name", table_name);
                    dialog.setArguments(args);
//                    dialog.show(getChildFragmentManager(), "add");
                    dialog.show(getFragmentManager(), "add");
                }
                break;
                case R.id.delete_current_page:{
                    EnsureDialogFragment dialog = EnsureDialogFragment.newInstance(
                            getString(R.string.warning),
                            getString(R.string.delete_current_page_warning)
                    );
                    dialog.setTargetFragment(TableSingleFragment.this,REQUEST_CODE_DELETE_CUR_PAGE);
//                    dialog.show(getChildFragmentManager(), "delete_cur_page");
                    dialog.show(getFragmentManager(), "delete_cur_page");
                }
                break;
                case R.id.clone:{
                    if (selected_row < 0) {
                        break;
                    }

                    showEditDialog(true,selected_row,dataGrid.getData().get(selected_row));
                }
                break;
            }
//        }else{
//            switch (item.getItemId()) {
//                case R.id.detail:
//                    showEditDialog(selected_row,dataGrid.getData().get(selected_row));
//                    break;
//            }
//        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * 修改(pro),显示(free)
     * @param values
     */
    private void showEditDialog(int selectedRow,ArrayList<String> values) {
        showEditDialog(false, selectedRow, values);
    }

    /**
     * 修改(pro),显示(free)
     * @param values
     * @param isClone 是否是复制选中行内容
     */
    private void showEditDialog(boolean isClone,int selectedRow,ArrayList<String> values) {
        TableEditDialogFragment dialog = new TableEditDialogFragment();
        dialog.setTargetFragment(TableSingleFragment.this,
                isClone ? REQUEST_CODE_ADD : REQUEST_CODE_EDIT);

        Bundle args = new Bundle();
        args.putStringArrayList("value",values);
        args.putString("table_name", table_name);
//				args.putStringArray("titles", table_fields);
        args.putInt("row", selectedRow);
        args.putInt("page",currentPage);
        args.putBoolean("isClone",isClone);
        args.putString("query_statement", query_statement);
        dialog.setArguments(args);
//        dialog.show(getChildFragmentManager(), "edit");
        dialog.show(getFragmentManager(), "edit");
    }

    private void saveToSourceData() {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... arg0) {
//                return Utils.runRootCommand("cat /sdcard/Android/data/cn.wq.myandroidtoolspro/cache/temp2.db > "+source_dir+"\n")
//                        ||Utils.runRootCommand("cat " + Utils.getTempDBPath(mContext) + " > " + source_dir + "\n")
//                        || Utils.runRootCommand("cat " + System.getenv("EXTERNAL_STORAGE") + "/Android/data/cn.wq.myandroidtoolspro/cache/temp2.db > " + source_dir + "\n");
                final String pkg = mContext.getPackageName();
                if (!hasWal) {
                    return Utils.runRootCommand("cat /sdcard/Android/data/"+ pkg +"/cache/temp2.db > "+fullPath+"\n")
                            || Utils.runRootCommand("cat " + Utils.getTempDBPath(mContext) + " > " + fullPath + "\n")
                            || Utils.runRootCommand("cat " + System.getenv("EXTERNAL_STORAGE") + "/Android/data/"+ pkg +"/cache/temp2.db > " + fullPath + "\n");
                }else{
                    return Utils.runRootCommand("cat /sdcard/Android/data/"+ pkg +"/cache/temp2.db > "+fullPath+"\n",
                                            "cat /sdcard/Android/data/"+ pkg +"/cache/temp2.db-wal > "+fullPath+"-wal\n")
                            || Utils.runRootCommand("cat " + Utils.getTempDBPath(mContext) + " > " + fullPath + "\n",
                                            "cat " + Utils.getTempDBWalPath(mContext) + " > " + fullPath + "-wal\n")
                            || Utils.runRootCommand("cat " + System.getenv("EXTERNAL_STORAGE") + "/Android/data/"+ pkg +"/cache/temp2.db > " + fullPath + "\n",
                                            "cat " + System.getenv("EXTERNAL_STORAGE") + "/Android/data/"+ pkg +"/cache/temp2.db-wal > " + fullPath + "-wal\n");
                }

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

}
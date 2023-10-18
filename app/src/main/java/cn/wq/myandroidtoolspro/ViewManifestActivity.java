package cn.wq.myandroidtoolspro;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.chainfire.libsuperuser.Debug;

@Deprecated
public class ViewManifestActivity extends BaseActivity{
    private String packageName;
    private List<String> mData = new ArrayList<>();
    private MyAdapter mAdapter;
    private ListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(PREFERENCE_DARK_THEME, false)) {
//            setTheme(R.style.AppBaseThemeDark);
//        } else {
//            setTheme(R.style.AppBaseTheme);
//        }
        final int theme = PreferenceManager.getDefaultSharedPreferences(this).getInt(PREFERENCE_THEME, 0);
        if (theme == 2) {
            setTheme(R.style.AppBaseThemeBlack);
        } else if (theme == 1) {
            setTheme(R.style.AppBaseThemeDark);
        } else {
            setTheme(R.style.AppBaseTheme);
        }

        super.onCreate(savedInstanceState);

        Bundle args=getIntent().getExtras();
        packageName = args.getString("packageName");

//        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_view_manifest);
        Toolbar toolbar= (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        String title = args.getString("title");
        if (TextUtils.isEmpty(title)) {
            getSupportActionBar().setTitle("Manifest");
        }else{
            getSupportActionBar().setTitle("Manifest("+title+")");
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mListView = (ListView) findViewById(R.id.listview);
        mAdapter = new MyAdapter();
        mListView.setAdapter(mAdapter);
        readManifest();
        mAdapter.notifyDataSetChanged();

//        TextView textView = (TextView) findViewById(R.id.text);
//        textView.setMovementMethod(ScrollingMovementMethod.getInstance());
//        textView.setText(readManifest());
    }

    private void readManifest() {
        try {
            PackageManager pm = getPackageManager();
            Context targetContent = this.createPackageContext(packageName, 0);
            AssetManager assetManager = targetContent.getAssets();
            String sourceDir = pm.getApplicationInfo(packageName, 0).sourceDir;

            Method addAssetPath = AssetManager.class.getMethod("addAssetPath",
                    String.class);
            int cookie = (Integer) addAssetPath.invoke(assetManager, sourceDir);
            XmlResourceParser parser = assetManager.openXmlResourceParser(
                    cookie, "AndroidManifest.xml");
            Resources resources = targetContent.getResources();
            int innerDepth=0;
            int type;
            while((type=parser.next())!= XmlPullParser.END_DOCUMENT) {
                if (
//                        type == XmlPullParser.END_TAG ||
                        type == XmlPullParser.TEXT) {
                    continue;
                }
//                if ("application".equals(parser.getName())) {
//                    innerDepth=parser.getDepth();
//                }
//                if (innerDepth==0||parser.getDepth() <= innerDepth) {
//                    continue;
//                }

                if (parser.getDepth() == 0) {
                    continue;
                }
                if(type==XmlPullParser.END_TAG){
                    StringBuilder sBuilder = new StringBuilder();

                    appendTab(sBuilder, parser.getDepth() - innerDepth - 1);
                    sBuilder.append("</").append(parser.getName()).append(">");
                    mData.add(sBuilder.toString());
                    continue;
                }

                if (parser.getAttributeCount() > 0) {
                    StringBuilder sBuilder = new StringBuilder();
                    appendTab(sBuilder, parser.getDepth() - innerDepth - 1);
                    sBuilder.append("<").append(parser.getName());
                    mData.add(sBuilder.toString());

                    for (int i = 0; i < parser.getAttributeCount(); i++) {
                        String attributeName = parser.getAttributeName(i);
//                        System.out.println("1 : " + i + " : " + attributeName);
                        if (TextUtils.isEmpty(attributeName)) {
                            int res = parser.getAttributeNameResource(i);
                            if (res != 0) {
                                attributeName = resources.getResourceEntryName(res);
//                                System.out.println("2 : " + i + " : " + attributeName);
                            }
                        }else{
//                            int res = parser.getAttributeNameResource(i);
//                            if (res != 0) {
//                                System.out.println(res+"!!!!!@@@!!! "+resources.getResourceEntryName(res)+" : "+resources.getResourceName(res)
//                                    +" : "+resources.getResourcePackageName(res)+" : "+resources.getResourceTypeName(res));
//                            }
                        }
                        //例如android:attr/windowAnimationStyle，其中getResourceName===getResourcePackageName:getResourceTypeName/getResourceEntryName

                        //解析命名空间http://schemas.android.com/apk/res/android为android
                        String nameSpace=parser.getAttributeNamespace(i);
                        int lastIndex=nameSpace.lastIndexOf("/");
                        if(lastIndex!=-1){
                            if(lastIndex!=nameSpace.length()-1){
                                //最后一个字符不是"/"
                                nameSpace=nameSpace.substring(lastIndex+1);
                            }else{
                                //最后一个字符是"/"
                                int lastSecondIndex=nameSpace.lastIndexOf("/",lastIndex-1);
                                if(lastSecondIndex!=-1){
                                    //倒数第二个"/"
                                    nameSpace=nameSpace.substring(lastSecondIndex+1,lastIndex);
                                }else{
                                    //前面没有"/"
                                    nameSpace=nameSpace.substring(0,lastSecondIndex);
                                }
                            }
                        }
//                        System.out.println(type+":"+parser.getDepth() + ":" + parser.getName()+ "~"+parser.getAttributeCount()+":" + parser.getAttributeNamespace(i)
//                                + " : " + attributeName + " | " + parser.getAttributeValue(i)+" : "+parser.getAttributeName(i)+" : "+parser.getNamespace()+" @ "
//                                +parser.getAttributeNameResource(i)+" : "+parser.getAttributeValue(parser.getAttributeNamespace(i),parser.getAttributeName(i))+":"+nameSpace);

                        //由@2131296311得到@style/AppTheme
                        //cn.wq.myandroidtoolspro:style/AppTheme
                        //resouceName=packageName:typeName/entryName
                        String value=parser.getAttributeValue(i);
                        String originalValue = value;
                        if(value.startsWith("@")){
                            value=value.substring(1);
                            try{
                                int res=Integer.parseInt(value);
                                value="@"+resources.getResourceTypeName(res)+"/"+resources.getResourceEntryName(res);

//                                System.out.println(res+"~~~~~~~~~~ "+resources.getResourceEntryName(res)+" : "+resources.getResourceName(res)
//                                        +" : "+resources.getResourcePackageName(res)+" : "+resources.getResourceTypeName(res));
                            }catch (Exception e){
                                if (BuildConfig.DEBUG) {
                                    e.printStackTrace();
                                }
                                value = originalValue;
                            }
                        }

                        sBuilder.delete(0, sBuilder.length());
                        appendTab(sBuilder, parser.getDepth() - innerDepth);

                        if(!TextUtils.isEmpty(nameSpace)){
                            sBuilder.append(nameSpace).append(":");
                        }
                        sBuilder.append(attributeName).append("=\"")
                                .append(value).append("\"");
                        if (i == parser.getAttributeCount() - 1) {
                            sBuilder.append(">");
                        }
//                        sBuilder.append("\n");
                        mData.add(sBuilder.toString());
                    }
                }else{
//                    System.out.println(type+" : "+parser.getDepth() + " : " + parser.getName());
                    StringBuilder sBuilder = new StringBuilder();
                    appendTab(sBuilder, parser.getDepth() - innerDepth);
                    sBuilder.append("<").append(parser.getName()).append(">");
                    mData.add(sBuilder.toString());
                }

            }
        } catch (PackageManager.NameNotFoundException | InvocationTargetException | IOException | NoSuchMethodException | IllegalAccessException | XmlPullParserException e) {
            e.printStackTrace();
        }
    }

//    private String readManifest() {
//        StringBuilder sBuilder = new StringBuilder();
//        try {
//            PackageManager pm = getPackageManager();
//            Context targetContent = this.createPackageContext(packageName, 0);
//            AssetManager assetManager = targetContent.getAssets();
//            String sourceDir = pm.getApplicationInfo(packageName, 0).sourceDir;
//
//            Method addAssetPath = AssetManager.class.getMethod("addAssetPath",
//                    String.class);
//            int cookie = (Integer) addAssetPath.invoke(assetManager, sourceDir);
//            XmlResourceParser parser = assetManager.openXmlResourceParser(
//                    cookie, "AndroidManifest.xml");
//            Resources resources = targetContent.getResources();
//            int innerDepth=0;
//            int type;
//            while((type=parser.next())!= XmlPullParser.END_DOCUMENT) {
//                if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
//                    continue;
//                }
//                if ("application".equals(parser.getName())) {
//                    innerDepth=parser.getDepth();
//                }
//                if (innerDepth==0||parser.getDepth() <= innerDepth) {
//                    continue;
//                }
//                if (parser.getAttributeCount() > 0) {
//                    appendTab(sBuilder, parser.getDepth() - innerDepth - 1);
//                    sBuilder.append("<").append(parser.getName()).append("\n");
//                    for (int i = 0; i < parser.getAttributeCount(); i++) {
//                        String attributeName = parser.getAttributeName(i);
//                        if (TextUtils.isEmpty(attributeName)) {
//                            int res=parser.getAttributeNameResource(i);
//                            if(res!=0){
//                                attributeName=resources.getResourceEntryName(res);
//                            }
//                        }
//
////                        System.out.println(parser.getDepth() + " : " + parser.getName()
////                                + " : " + parser.getAttributeNamespace(i) + " : " + attributeName + " : " + parser.getAttributeValue(i));
//
//                            appendTab(sBuilder, parser.getDepth() - innerDepth);
//                            sBuilder.append("android:").append(attributeName).append("=\"")
//                                    .append(parser.getAttributeValue(i)).append("\"");
//                            if (i == parser.getAttributeCount() - 1) {
//                                sBuilder.append(">");
//                            }
//                            sBuilder.append("\n");
//
//                    }
//                }else{
////                    System.out.println(parser.getDepth() + " : " + parser.getName());
//                    appendTab(sBuilder, parser.getDepth() - innerDepth);
//                    sBuilder.append("<").append(parser.getName()).append(">\n");
//                }
//
//            }
////            System.out.println("-----------------------");
////            System.out.println(sBuilder.toString());
//        } catch (PackageManager.NameNotFoundException | InvocationTargetException | IOException | NoSuchMethodException | IllegalAccessException | XmlPullParserException e) {
//            e.printStackTrace();
//        }
//            return sBuilder.toString();
//
//    }

    //TODO:用普通空格会导致由于英文排版而产生空行
    private void appendTab(StringBuilder sb, int depth) {
        for (int i = depth; i >0; i--) {
//            sb.append("\t");
            //xml中是&#160;
            sb.append((char)(0xA0)).append((char)(0xA0));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem menuItem=menu.add(0, R.id.search, 0, R.string.search);
        MenuItemCompat.setShowAsAction(menuItem,MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
        menuItem.setIcon(R.drawable.ic_search_white_24dp);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.search:
                startSupportActionMode(mActionModeCallback);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    class Position{
       int row=-1;
       int columnIndex;
    }
    private class MyAdapter extends BaseAdapter{
        private SparseArray<List<Integer>> searchPos=new SparseArray<>();
        private int searchTotal;
        private String query;
        private Position searchCurrent=new Position();

        public void search(String query) {
            this.query=query;
            searchPos.clear();
            if (TextUtils.isEmpty(query)) {
                searchTotal=0;
                searchCurrent.row=-1;
                searchCurrent.columnIndex=0;
                notifyDataSetChanged();
                return;
            }
            int size=mData.size();

            searchTotal=0;
            int smallestRow=0;
            for (int i=0;i<size;i++) {
                List<Integer> columns = new ArrayList<>();
                String data = mData.get(i).toLowerCase();   //搜索忽略大小写
                int index=0;
                String lowerQuery=query.toLowerCase();
                while (true) {
                    index = data.indexOf(lowerQuery, index);
                    if (index >= 0) {
                        if (smallestRow == 0) {
                            searchCurrent.row=smallestRow=i;
                            searchCurrent.columnIndex=index;
                        }
                        columns.add(index);
                        index+=query.length();
                        searchTotal++;
                    }else{
                        break;
                    }
                }
                if (columns.size() > 0) {
                    searchPos.put(i, columns);
                }else{
                    columns.clear();
                }
            }

            if (searchTotal == 0) {
                //reset to default
                searchCurrent.row=-1;
                smallestRow=0;
            }
//            notifyDataSetChanged();
//            mListView.smoothScrollToPosition(smallestRow);
            notifyDataSetChanged();
            mListView.setSelection(smallestRow);
        }

        public void previous() {
            if (searchCurrent.row < 0) {
                return;
            }
            List<Integer> currentColumns = searchPos.get(searchCurrent.row);
            int innerIndex = currentColumns.indexOf(searchCurrent.columnIndex);
            //previous is in an other row
            if (innerIndex ==0) {
                int outerIndex=searchPos.indexOfKey(searchCurrent.row);
                if (outerIndex == 0) {
                    searchCurrent.row=searchPos.keyAt(searchPos.size()-1);

                    List<Integer> columns = searchPos.get(searchCurrent.row);
                    searchCurrent.columnIndex=searchPos.get(searchCurrent.row).get(columns.size()-1);
                    //不用调用notifyDataSetChanged
                    mListView.setSelection(searchCurrent.row);
                }else{
                    searchCurrent.row = searchPos.keyAt(outerIndex-1);

                    List<Integer> columns = searchPos.get(searchCurrent.row);
                    searchCurrent.columnIndex=searchPos.get(searchCurrent.row).get(columns.size()-1);
                    mListView.smoothScrollToPosition(searchCurrent.row);
                    notifyDataSetChanged();
                }
            }else{
                searchCurrent.columnIndex = currentColumns.get(innerIndex - 1);
                notifyDataSetChanged();
            }
        }

        public void next() {
            if (searchCurrent.row < 0) {
                return;
            }
            List<Integer> currentColumns = searchPos.get(searchCurrent.row);
            int innerIndex = currentColumns.indexOf(searchCurrent.columnIndex);
            //next is in an other row
            if (innerIndex ==currentColumns.size()-1) {
                int outerIndex=searchPos.indexOfKey(searchCurrent.row);
                if (outerIndex == searchPos.size()-1) {
                    searchCurrent.row=searchPos.keyAt(0);

                    searchCurrent.columnIndex=searchPos.get(searchCurrent.row).get(0);
                    mListView.setSelection(searchCurrent.row);
                }else{
                    searchCurrent.row = searchPos.keyAt(outerIndex + 1);

                    searchCurrent.columnIndex=searchPos.get(searchCurrent.row).get(0);
                    mListView.smoothScrollToPosition(searchCurrent.row);
                    notifyDataSetChanged();
                }

            }else{
                searchCurrent.columnIndex = currentColumns.get(innerIndex+1);
                notifyDataSetChanged();
            }

        }

        public void reset() {
            searchPos.clear();
            searchCurrent.row=-1;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public Object getItem(int position) {
            return mData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = new TextView(parent.getContext());
                TextView tv=(TextView) convertView;
                tv.setTextSize(14);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    tv.setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    tv.setTextIsSelectable(true);
                }
                tv.setSingleLine(false);
            }
            TextView textView= (TextView) convertView;
            List<Integer> columns = searchPos.get(position);
            if (columns != null && columns.size() > 0) {
                SpannableString spannableString = new SpannableString(mData.get(position));
                for(int i=0;i<columns.size();i++) {
                    if (searchCurrent.row == position && searchCurrent.columnIndex == columns.get(i)) {
                        spannableString.setSpan(new BackgroundColorSpan(Color.parseColor("#7FFF7F00")),
                                columns.get(i),
                                columns.get(i)+query.length(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }else{
                        spannableString.setSpan(new BackgroundColorSpan(Color.parseColor("#7FFFFF00")),
                                columns.get(i),
                                columns.get(i)+query.length(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }

                }
                textView.setText(spannableString);
            }else{
                textView.setText(mData.get(position));
            }
            return convertView;
        }
    }

    private ActionMode.Callback mActionModeCallback=new ActionMode.Callback() {
        private EditText mEditText;
        private ImageButton previousBtn,nextBtn;

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            if (Build.VERSION.SDK_INT >= 21) {
                getWindow().setStatusBarColor(ContextCompat.getColor(ViewManifestActivity.this,R.color.blue_grey_700));
            }
            View view=LayoutInflater.from(ViewManifestActivity.this).inflate(R.layout.actionmode_search,null);
            mEditText = (EditText) view.findViewById(R.id.edittext);
             previousBtn = (ImageButton) view.findViewById(R.id.previous);
             nextBtn = (ImageButton) view.findViewById(R.id.next);

            mEditText.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mEditText.requestFocus();
                    InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    inputMethodManager.showSoftInput(mEditText, InputMethodManager.SHOW_IMPLICIT);
                }
            },0);
            mEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        hideInputMethod();
                    }
                    return true;
                }
            });
            try {
                Field f = TextView.class.getDeclaredField("mCursorDrawableRes");
                f.setAccessible(true);
                f.set(mEditText, R.drawable.edittext_cursor);
            } catch (Exception ignored) {
            }

            mEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }
                @Override
                public void afterTextChanged(Editable s) {
                    mAdapter.search(s.toString());
                }
            });

            previousBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    hideInputMethod();
                    mAdapter.previous();
                }
            });
            nextBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    hideInputMethod();
                    mAdapter.next();
                }
            });
            mode.setCustomView(view);
            return true;
        }

        private void hideInputMethod() {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            if (Build.VERSION.SDK_INT >= 21) {
                getWindow().setStatusBarColor(ContextCompat.getColor(ViewManifestActivity.this,R.color.actionbar_color_dark));
            }
            hideInputMethod();
            mAdapter.reset();
        }
    };
}

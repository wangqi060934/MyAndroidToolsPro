package cn.wq.myandroidtoolspro;


import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import cn.wq.myandroidtoolspro.helper.Utils;

public class ViewManifestActivity2 extends BaseActivity {
    private static final String TAG = "ViewManifestActivity2";
    private String packageName;
    private WebView mWebView;
    private AsyncTask<Void,Void,String> mTask;
    private ProgressBar mProgressBar;
    private ActionBar mActionBar;
    private MyHandler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final int theme = PreferenceManager.getDefaultSharedPreferences(this).getInt(PREFERENCE_THEME, 0);
        if (theme == 2) {
            setTheme(R.style.AppBaseThemeBlack);
        } else if (theme == 1) {
            setTheme(R.style.AppBaseThemeDark);
        } else {
            setTheme(R.style.AppBaseTheme);
        }
//        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(PREFERENCE_DARK_THEME, false)) {
//            setTheme(R.style.AppBaseThemeDark);
//        } else {
//            setTheme(R.style.AppBaseTheme);
//        }

        super.onCreate(savedInstanceState);

        Bundle args=getIntent().getExtras();
        if (args != null) {
            packageName = args.getString("packageName");
        }

        setContentView(R.layout.activity_view_manifest2);
        mWebView = (WebView) findViewById(R.id.webview);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        if (PreferenceManager.getDefaultSharedPreferences(this).getInt(BaseActivity.PREFERENCE_THEME, 0) >0) {
            mProgressBar.setIndeterminateDrawable(ContextCompat.getDrawable(this,R.drawable.progress_dark));
        }
        Toolbar toolbar= (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mActionBar = getSupportActionBar();
        if (mActionBar != null) {
            mActionBar.setTitle("Manifest");
            mActionBar.setDisplayHomeAsUpEnabled(true);
        }
        setToolbarLogo(packageName);

        mTask=new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                return readManifest();
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                if (s == null) {
                    Toast.makeText(ViewManifestActivity2.this,R.string.operation_failed,Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }

                //参考 https://github.com/google/code-prettify
                mWebView.getSettings().setJavaScriptEnabled(true);
                mWebView.setWebViewClient(new WebViewClient(){
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        super.onPageFinished(view, url);
                        if (url.startsWith("file:///android_asset/")) {
                            mWebView.setVisibility(View.VISIBLE);
                            mProgressBar.setVisibility(View.GONE);
                        }
                    }
                });
                mWebView.setBackgroundColor(0);
                String data = "<script src=\"./run_prettify.js?skin=sons-of-obsidian\"></script><body bgcolor=\"#000\"><pre class=\"prettyprint linenums\">"
                        + s + "</pre></body>";
                //默认的黑色背景只显示开始的一个屏幕大小，滑动到其它地方都是白色背景，需要用body的bgcolor
                //参考 https://github.com/apache/commons-text/blob/master/src/main/java/org/apache/commons/text/StringEscapeUtils.java 处理xml中转义字符
                mWebView.loadDataWithBaseURL(
                        "file:///android_asset/",
                        data,
                        "text/html",
                        "utf-8",
                        null);
//                if (Build.VERSION.SDK_INT >= 16) {
//                    mWebView.setFindListener(new WebView.FindListener() {
//                        @Override
//                        public void onFindResultReceived(int activeMatchOrdinal, int numberOfMatches, boolean isDoneCounting) {
//                            //activeMatchOrdinal 当前第几个，0开始;findNext后需要
//                            //numberOfMatches 总个数;
//                            //isDoneCounting 是否统计完
//                        }
//                    });
//                }

                mHandler = new MyHandler(mWebView);
            }
        };
        mTask.execute();
    }

    private String readManifest() {
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

            StringBuilder resultBuilder = new StringBuilder();
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
//                    sBuilder.append("</").append(parser.getName()).append(">");
//                    sBuilder.append("\r\n");
                    sBuilder.append("&lt;/").append(parser.getName()).append("&gt;").append("\r\n");
                    resultBuilder.append(sBuilder.toString());
                    continue;
                }

                if (parser.getAttributeCount() > 0) {
                    StringBuilder sBuilder = new StringBuilder();
                    appendTab(sBuilder, parser.getDepth() - innerDepth - 1);
//                    sBuilder.append("<").append(parser.getName());
                    sBuilder.append("&lt;").append(parser.getName());
                    resultBuilder.append(sBuilder.toString()).append("\r\n");

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
                                    nameSpace = nameSpace.substring(0, lastIndex - 1);
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
                        sBuilder.append(attributeName).append("=\"").append(value).append("\"");
//                        sBuilder.append(attributeName).append("=").append("&quot;").append(value).append("&quot;");
                        if (i == parser.getAttributeCount() - 1) {
//                            sBuilder.append(">");
                            sBuilder.append("&gt;");
                        }
//                        sBuilder.append("\n");
                        resultBuilder.append(sBuilder.toString()).append("\r\n");
                    }
                }else{
//                    System.out.println(type+" : "+parser.getDepth() + " : " + parser.getName());
                    StringBuilder sBuilder = new StringBuilder();
                    appendTab(sBuilder, parser.getDepth() - innerDepth);
//                    sBuilder.append("<").append(parser.getName()).append(">");
                    sBuilder.append("&lt;").append(parser.getName()).append("&gt;");
                    resultBuilder.append(sBuilder.toString()).append("\r\n");
                }

            }
//            return StringEscapeUtils.escapeXml10(resultBuilder.toString());
            return resultBuilder.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //用普通空格会导致由于英文排版而产生空行
    private void appendTab(StringBuilder sb, int depth) {
        for (int i = depth; i >0; i--) {
            sb.append("   ");
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

    private ActionMode.Callback mActionModeCallback=new ActionMode.Callback() {
        private EditText mEditText;
        private ImageButton previousBtn,nextBtn;

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            if (Build.VERSION.SDK_INT >= 21) {
                getWindow().setStatusBarColor(ContextCompat.getColor(ViewManifestActivity2.this,R.color.blue_grey_700));
            }
            View view= LayoutInflater.from(ViewManifestActivity2.this).inflate(R.layout.actionmode_search,null);
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
                private long lastTime;
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }
                @Override
                public void afterTextChanged(final Editable s) {
                    long now = System.currentTimeMillis();
                    if (now - lastTime < 1000) {
                        mHandler.removeMessages(1);
                        return;
                    }
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Message msg = Message.obtain(mHandler, 1);
                            msg.obj = s.toString();
                            mHandler.sendMessage(msg);
                        }
                    }, 1000);
                    lastTime = now;

                }
            });

            previousBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    hideInputMethod();
                    if (mWebView.isShown()) {
                        mWebView.findNext(false);
                    }
                }
            });
            nextBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    hideInputMethod();
                    if (mWebView.isShown()) {
                        mWebView.findNext(true);
                    }
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
                getWindow().setStatusBarColor(ContextCompat.getColor(ViewManifestActivity2.this,R.color.actionbar_color_dark));
            }
            hideInputMethod();
            mWebView.clearMatches();
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mTask != null) {
            mTask.cancel(true);
        }
    }

    private void setToolbarLogo(String packageName) {
        if (mActionBar == null || TextUtils.isEmpty(packageName)) {
            return;
        }
        try {
            int length = (int) (getResources().getDisplayMetrics().density * 24);
            Drawable icon=getPackageManager().getApplicationIcon(packageName);
                Toolbar toolbar= (Toolbar) findViewById(R.id.toolbar);
                toolbar.setLogo(icon);
                for (int i = 0; i < toolbar.getChildCount(); i++) {
                    View child = toolbar.getChildAt(i);
                    if (child != null && child instanceof ImageView){
                        ImageView iv = (ImageView) child;
                        if ( iv.getDrawable() == icon ) {
                            ViewGroup.LayoutParams params=iv.getLayoutParams();
                            params.width=params.height=length;
                        }
                    }
                }

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static class MyHandler extends Handler{
        private WeakReference<WebView> mWeakReference;

        MyHandler(WebView v) {
            mWeakReference = new WeakReference<>(v);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            WebView webView = mWeakReference.get();
            if (webView == null) {
                return;
            }
            if (msg.what == 1) {
                if(Build.VERSION.SDK_INT>=16){
                    webView.findAllAsync(msg.obj.toString());
                }else{
                    webView.findAll(msg.obj.toString());
                }
            }
        }
    }

}

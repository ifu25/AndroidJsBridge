package cc.wco.jsbridgedemo;

import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.Toast;

import cc.wco.jsbridge.BridgeHandler;
import cc.wco.jsbridge.BridgeWebView;
import cc.wco.jsbridge.CallBackFunction;
import cc.wco.jsbridge.DefaultHandler;

/**
 * 名称：JsBridge 编程演示（Js与Java互相调用）
 * 作者：邢港
 * 日期：2018/05/03
 * 说明：通过JsBridge实现js调用android方法、android调用js方法，并且可以相互传递数据及实现回调（如扫描二维码成功后再回调js相应的函数）
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    //常用变量定义
    private final String TAG = "ifu25";
    private Button btnRefreshURL;
    private Button btnScanNative;
    private Button btnJavaCallJs;

    private static final int REQUEST_CODE_QRCODE = 1; //扫描请求码

    //JsBridge相关变量
    private BridgeWebView mWebView;
    private static CallBackFunction mfunction; //回调函数
    private static String mUrl = "file:///android_asset/demo.html"; //"file:///android_asset/demo.html"; //https://app.lttc.cn/demo.html

    //上传文件相关变量
    private ValueCallback<Uri> mUploadMessage;
    public ValueCallback<Uri[]> mUploadMessageAfterL;
    public static final int REQUEST_CODE_SELECT_FILE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //初始化视图
        initView();

        //初始化WebView
        initWebView();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    /**
     * 初始化视图
     */
    private void initView() {
        btnRefreshURL = findViewById(R.id.btnRefreshURL);
        btnScanNative = findViewById(R.id.btnScanNative);
        btnJavaCallJs = findViewById(R.id.btnJavaCallJs);
        btnRefreshURL.setOnClickListener(this);
        btnScanNative.setOnClickListener(this);
        btnJavaCallJs.setOnClickListener(this);
    }

    /**
     * 初始化webView
     */
    private void initWebView() {
        mWebView = findViewById(R.id.webView);
        mWebView.setDefaultHandler(new DefaultHandler());
        mWebView.setWebChromeClient(new WebChromeClient() {

            //这里的几个方法用于解决web中type="file"的按钮不能选择文件的问题

            protected void openFileChooser(ValueCallback<Uri> uploadMsg) {
                mUploadMessage = uploadMsg;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, "文件浏览"), REQUEST_CODE_SELECT_FILE);
            }

            // For 3.0+ Devices (Start)
            protected void openFileChooser(ValueCallback uploadMsg, String acceptType) {
                mUploadMessage = uploadMsg;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, "文件浏览"), REQUEST_CODE_SELECT_FILE);
            }

            // For Android 4.1 only
            protected void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
                mUploadMessage = uploadMsg;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, "文件浏览"), REQUEST_CODE_SELECT_FILE);
            }

            // For Lollipop 5.0+ Devices
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
                if (mUploadMessageAfterL != null) {
                    mUploadMessageAfterL.onReceiveValue(null);
                    mUploadMessageAfterL = null;
                }

                mUploadMessageAfterL = filePathCallback;

                Intent intent = fileChooserParams.createIntent();
                try {
                    startActivityForResult(intent, REQUEST_CODE_SELECT_FILE);
                } catch (ActivityNotFoundException ex) {
                    mUploadMessageAfterL = null;
                    Toast.makeText(getBaseContext(), "打开文件浏览出错" + ex.getMessage(), Toast.LENGTH_LONG).show();
                    return false;
                }

                return true;
            }

        });
        mWebView.loadUrl(mUrl);

        //注册供JS调用的方法：方法名、参数数据、回调函数
        mWebView.registerHandler("login", new BridgeHandler() {
            @Override
            public void handler(String data, CallBackFunction function) {
                function.onCallBack("登录成功(来自java的响应！)");
            }
        });

        //注册供JS调用的方法：方法名、参数数据、回调函数
        mWebView.registerHandler("scanQRCode", new BridgeHandler() {
            @Override
            public void handler(String data, CallBackFunction function) {

                //扫描二维码
                //Intent openCameraIntent = new Intent(MainActivity.this, CaptureActivity.class);
                //startActivityForResult(openCameraIntent, REQUEST_CODE_QRCODE);

                //设置回调函数，扫描后在 onActivityResult 中触发
                mfunction = function;
            }
        });

        //发送消息给js
        mWebView.send("hello");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        //返回二维码扫描结果
        if (requestCode == REQUEST_CODE_QRCODE) {
            String scanResult;
            if (resultCode == RESULT_OK) {
                scanResult = intent.getExtras().getString("result"); //取扫描结果
            } else {
                scanResult = "未扫描到二维码！";
            }

            //回调js方法，并把结果传回js
            if (mfunction != null) {
                mfunction.onCallBack(scanResult);
            }
            //回调为null，则弹出toast
            else {
                Toast.makeText(this, scanResult, Toast.LENGTH_SHORT).show();
            }
        }
        //返回文件选择结果
        else if (requestCode == REQUEST_CODE_SELECT_FILE) {
            //Android >= 5.0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (mUploadMessageAfterL == null) return;
                mUploadMessageAfterL.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, intent));
                mUploadMessageAfterL = null;
            }
            //Android < 5.0
            else {
                if (mUploadMessage == null) return;

                // Use MainActivity.RESULT_OK if you're implementing WebView inside Fragment
                // Use RESULT_OK only if you're implementing WebView inside an Activity
                Uri result = intent == null || resultCode != MainActivity.RESULT_OK ? null : intent.getData();
                mUploadMessage.onReceiveValue(result);
                mUploadMessage = null;
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnRefreshURL:
                mWebView.reload();

                break;
            case R.id.btnScanNative:
                //扫描二维码
                //Intent openCameraIntent = new Intent(MainActivity.this, CaptureActivity.class);
                //startActivityForResult(openCameraIntent, REQUEST_CODE_QRCODE);
                mfunction = null;

                break;
            case R.id.btnJavaCallJs:
                //java调用js，方法名、参数数据、回调函数
                mWebView.callHandler("functionInJs", "我是从Java传来的数据", new CallBackFunction() {

                    @Override
                    public void onCallBack(String data) {
                        //这里的data是js传来的
                        Log.d(TAG, data);
                        Toast.makeText(MainActivity.this, data, Toast.LENGTH_SHORT).show();
                    }
                });

                break;
        }
    }
}

package cn.jiguang.example.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;

import cn.jiguang.R;
import cn.jiguang.example.ui.base.BaseActivity;
import cn.jiguang.example.util.DownloadUtils;
import cn.jiguang.iot.JiotClient;
import cn.jiguang.iot.api.JiotClientApi;
import cn.jiguang.iot.bean.DeviceInfo;
import cn.jiguang.iot.bean.Event;
import cn.jiguang.iot.bean.EventReportReq;
import cn.jiguang.iot.bean.EventReportRsp;
import cn.jiguang.iot.bean.JiotResult;
import cn.jiguang.iot.bean.MsgDeliverReq;
import cn.jiguang.iot.bean.OtaStatusReportReq;
import cn.jiguang.iot.bean.OtaStatusReportRsp;
import cn.jiguang.iot.bean.Property;
import cn.jiguang.iot.bean.PropertyReportReq;
import cn.jiguang.iot.bean.PropertyReportRsp;
import cn.jiguang.iot.bean.PropertySetReq;
import cn.jiguang.iot.bean.OtaUpgradeInformReq;
import cn.jiguang.iot.bean.VersionReportReq;
import cn.jiguang.iot.bean.VersionReportRsp;
import cn.jiguang.iot.callback.JclientHandleCallback;
import cn.jiguang.iot.callback.JclientMessageCallback;
import cn.jiguang.iot.util.JiotLogger;

/**
 * @author: ouyangshengduo
 * e-mail: ouysd@jiguang.cn
 * date  : 2019/4/9 10:29
 * desc  :
 */
public class MainActivity extends BaseActivity implements JclientHandleCallback, JclientMessageCallback, View.OnClickListener {

    private JiotClientApi jiotClientApi;
    /**
     * 运行日志
     */
    private TextView tvLogInfo;
    /**
     * 设备三元组输入之产品key
     */
    private EditText etProductKey;
    /**
     * 设备三元组输入之设备名称
     */
    private EditText etDeviceName;
    /**
     * 设备三元组输入之设备密钥
     */
    private EditText etDeviceSecret;
    /**
     * 设备属性或者事件的名称
     */
    private EditText etPropertyEventName;
    /**
     * 设备属性或者事件的值
     */
    private EditText etValueContent;
    /**
     * 设备上报版本
     */
    private EditText etVersion;
    /**
     * 设备信息布局
     */
    private LinearLayout llDeviceInfo;

    /**
     * 升级布局
     */
    private LinearLayout llUpgradeInfo;
    /**
     * 升级进度显示
     */
    private TextView tvUpgradeProcess;
    private SharedPreferences preferences = null;
    private  static final String DEVICE_INFO = "DEVICE_INFO";
    private String localProductKey;
    private String localDeviceName;
    private String localDeviceSecret;
    private String localPropertyOrEventName;
    private SimpleDateFormat simpleDateFormat;

    private String otaUrl;
    private String otaMd5;
    private static final String STORAGE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();
    private long otaTaskId;

    private CallbackHandler mHandler = new CallbackHandler(this);
    private static class CallbackHandler extends Handler {
        private WeakReference<Context> reference;
        private MainActivity mainActivity;

        /** 静态内部类构造方法
         * @param context 上下文
         */
        CallbackHandler(Context context){
            reference = new WeakReference<>(context);
        }

        @Override
        public void handleMessage(Message msg) {
            mainActivity = (MainActivity) reference.get();
            if(mainActivity != null){
                switch (msg.what){
                    case Constant.CONNECTED_HANDLE:
                        mainActivity.showLogInfo("Client connected. \n");
                        break;
                    case Constant.CONNECT_FAILE_HANDLE:
                        mainActivity.showDisconnectStatusUI();
                        mainActivity.showLogInfo("Client connect fail,error code is " + msg.arg1 + ". \n");
                        break;
                    case Constant.GET_CONN_STATUS:
                        mainActivity.showLogInfo("Client connect status is " + Constant.CONN_STATUS[msg.arg1] + ". \n");
                        break;
                    case Constant.DISCONNECT_HANDLE:
                        mainActivity.showDisconnectStatusUI();
                        mainActivity.showLogInfo("Client disconnect " + " errcode = " + msg.arg1 + ". \n");
                        break;
                    case Constant.SUBSCRIBE_FAIL_HANDLE:
                        mainActivity.showLogInfo("Client subscribe fail and topic = " + msg.obj + ". \n");
                        break;
                    case Constant.MESSAGE_TIME_OUT_HANDLE:
                        mainActivity.showLogInfo("Client send message timeout and seq_no = " + msg.obj + ". \n");
                        break;
                    case Constant.PUBLISH_FAIL_HANDLE:
                        mainActivity.showLogInfo("Client publish fail and seq_no = " + msg.obj + ". \n");
                        break;
                    case Constant.REPORT_VERSION_RESPONSE:
                        VersionReportRsp versionReportRsp = (VersionReportRsp) msg.obj;
                        if(versionReportRsp != null) {
                            mainActivity.showLogInfo("Client receive message (about report verion) from server" + " errcode = " + msg.arg1 + " code = " + versionReportRsp.getCode() + " seq_no = " + versionReportRsp.getSeqNo() + ". \n");
                        }else{
                            mainActivity.showLogInfo("Client receive message (about report verion) from server" + " errcode = " + msg.arg1 + " ,json object is null" + ". \n");
                        }
                        break;
                    case Constant.REPORT_PROPERTY_RESPONSE:
                        PropertyReportRsp propertyReportRsp = (PropertyReportRsp) msg.obj;
                        if(propertyReportRsp != null) {
                            mainActivity.showLogInfo("Client receive message (about report property) from server" + " errcode = " + msg.arg1 + " code = " + propertyReportRsp.getCode() + " seq_no = " + propertyReportRsp.getSeqNo() + " version = " + propertyReportRsp.getVerion() + " property_size = " + propertyReportRsp.getProperties().length + ". \n");
                        }else{
                            mainActivity.showLogInfo("Client receive message (about report property) from server" + " errcode = " + msg.arg1 + " ,json object is null" + ". \n");
                        }
                        break;
                    case Constant.REPORT_EVENT_RESPONSE:
                        EventReportRsp eventReportRsp = (EventReportRsp) msg.obj;
                        if(eventReportRsp != null) {
                            mainActivity.showLogInfo("Client receive message (about report event) from server" + " errcode = " + msg.arg1 + " code = " + eventReportRsp.getCode() + " seq_no = " + eventReportRsp.getSeqNo() + ". \n");
                        }else{
                            mainActivity.showLogInfo("Client receive message (about report event) from server" + " errcode = " + msg.arg1 + " ,json object is null" + ". \n");
                        }
                        break;
                    case Constant.MSG_DELIVER_REQ:
                        MsgDeliverReq msgDeliverReq = (MsgDeliverReq) msg.obj;
                        if(msgDeliverReq != null) {
                            mainActivity.showLogInfo("Client receive message (about msg deliver) from server" + " errcode = " + msg.arg1 + " seq_no = " + msgDeliverReq.getSeqNo() + " message = " + msgDeliverReq.getMessage() + " timestamp = " + msgDeliverReq.getTime() + ". \n");
                        }else{
                            mainActivity.showLogInfo("Client receive message (about msg deliver) from server" + " errcode = " + msg.arg1 + " ,json object is null" + ". \n");
                        }
                        break;
                    case Constant.PROPERTY_SET_REQ:
                        PropertySetReq propertySetReq = (PropertySetReq) msg.obj;
                        if(propertySetReq != null) {
                            mainActivity.showLogInfo("Client receive message (about set property) from server" + " errcode = " + msg.arg1 + " seq_no = " + propertySetReq.getSeqNo() + " verion = " + propertySetReq.getVersion() + " property_size = " + propertySetReq.getProperties().length + ". \n");
                        }else{
                            mainActivity.showLogInfo("Client receive message (about set property) from server" + " errcode = " + msg.arg1 + " ,json object is null" + ". \n");
                        }
                        break;
                    case Constant.CONNECTING:
                        mainActivity.showLogInfo("Client connecting... " + " \n");
                        break;
                    case Constant.DISCONNECTING:
                        mainActivity.showLogInfo("Client disconnecting... " + " \n");
                        break;
                    case Constant.CONNECT_RESPONSE:
                        mainActivity.showDisconnectStatusUI();
                        mainActivity.showLogInfo("Client connect response code = " + msg.arg1 + " \n");
                        break;
                    case Constant.OTA_UPGRADE_INFORM_REQ:
                        mainActivity.showUpgradeInfoUI();
                        OtaUpgradeInformReq otaUpgradeInformReq = (OtaUpgradeInformReq) msg.obj;
                        if(otaUpgradeInformReq != null) {
                            mainActivity.otaUrl = otaUpgradeInformReq.getData().getUrl();
                            mainActivity.otaMd5 = otaUpgradeInformReq.getData().getMd5();
                            mainActivity.otaTaskId = otaUpgradeInformReq.getData().getTaskId();
                            mainActivity.showLogInfo("Client receive message (about ota upgrade inform) from server" + " errcode = " + msg.arg1 + " seq_no = " + otaUpgradeInformReq.getSeqNo() + " data = " + otaUpgradeInformReq.getData().toString() + ". \n");
                        }else{
                            mainActivity.showLogInfo("Client receive message (about ota upgrade inform) from server" + " errcode = " + msg.arg1 + ". \n");
                        }
                        break;
                    case Constant.OTA_SHOW_PROGRESS:
                        mainActivity.tvUpgradeProcess.setText( msg.arg1 + "%");
                        break;
                    case Constant.REPORT_OTA_STATUS_RESPONSE:
                        OtaStatusReportRsp otaStatusReportRsp = (OtaStatusReportRsp) msg.obj;
                        if(otaStatusReportRsp != null) {
                            mainActivity.showLogInfo("Client receive message (about report ota status) from server" + " errcode = " + msg.arg1 + " code = " + otaStatusReportRsp.getCode() + " seq_no = " + otaStatusReportRsp.getSeqNo() + ". \n");
                        }else{
                            mainActivity.showLogInfo("Client receive message (about report ota status) from server" + " errcode = " + msg.arg1 + " ,json object is null" + ". \n" );
                        }
                        break;
                    case Constant.OTA_MD5_CHECK_SUCCESS:
                        String outputFile = (String)msg.obj;
                        mainActivity.checkApp(new File(outputFile));
                        break;
                    default:
                        break;
                }
            }
        }


    }

    /**
     * 当收到升级请求后UI的模拟处理
     */
    private void showUpgradeInfoUI() {
        if(llUpgradeInfo != null){
            llUpgradeInfo.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 当升级完成，隐藏布局
     */
    private void hideUpgradeInfoUI() {
        if(llUpgradeInfo != null){
            llUpgradeInfo.setVisibility(View.GONE);
        }
    }

    /**
     * 显示日志
     * @param content 日志内容
     */
    private void showLogInfo(String content){
        long currentTime = System.currentTimeMillis();
        String timeNow = simpleDateFormat.format(currentTime);
        tvLogInfo.append( timeNow + ":  " + content);
    }

    /**
     * 设备连接失败的UI变化
     */
    private void showDisconnectStatusUI() {
        llDeviceInfo.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            finish();
            return;
        }
        setContentView(R.layout.activity_main);

        initView();
        initData();
        requestPermission();
    }

    private void requestPermission() {
        //检查权限是否存在
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            //向用户申请授权
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            }, 1);

        }
    }

    /**
     * 数据对象的初始化
     */
    @SuppressLint("SimpleDateFormat")
    private void initData() {
        preferences = getSharedPreferences(DEVICE_INFO, Activity.MODE_PRIVATE);
        localProductKey = preferences.getString("productKey",null);
        localDeviceName = preferences.getString("deviceName",null);
        localDeviceSecret = preferences.getString("deviceSecret",null);
        localPropertyOrEventName = preferences.getString("propertyOrEventName",null);
        jiotClientApi = JiotClient.getInstance();
        jiotClientApi.jiotInit(this,false);
        etProductKey.setText(localProductKey == null ? Constant.DEFAULT_PRODUCT_KEY : localProductKey);
        etDeviceName.setText(localDeviceName == null ? Constant.DEFAULT_DEVICE_NAME : localDeviceName);
        etDeviceSecret.setText(localDeviceSecret == null ? Constant.DEFAULT_DEVICE_SECRET : localDeviceSecret);
        etVersion.setText(Constant.DEFAULT_REPORT_VERSION);
        etPropertyEventName.setText(localPropertyOrEventName == null ? Constant.DEFAULT_DEVICE_PROPERTY : localPropertyOrEventName);
        etValueContent.setText(Constant.DEFAULT_REPORT_CONTENT);

        simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

    /**
     * 界面UI控件的初始化
     */
    private void initView() {
        //连接服务器
        Button btnConnect = (Button) findViewById(R.id.btn_connect);
        //断开连接
        Button btnDisconnect = (Button) findViewById(R.id.btn_disconnect);
        //获取连接状态
        Button btnGetConnStatus = (Button) findViewById(R.id.btn_get_conn_status);
        //上报设备属性
        Button btnReportProperty = (Button) findViewById(R.id.btn_report_property);
        //上报设备事件
        Button btnReportEvent = (Button) findViewById(R.id.btn_report_event);
        //上报设备版本
        Button btnReportVersion = (Button) findViewById(R.id.btn_report_version);
        // 开始升级
        Button btnStartUpgrade = (Button) findViewById(R.id.btn_start_upgrade);
        // 测试专用，根据具体的功能实现
        Button btnTest = findViewById(R.id.btn_test);

        tvUpgradeProcess = (TextView) findViewById(R.id.tv_upgrade_process);
        llUpgradeInfo = (LinearLayout) findViewById(R.id.ll_upgrade_info);
        tvLogInfo = (TextView) findViewById(R.id.tv_log_info);
        //logcat运行日志等级
        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        etProductKey = (EditText) findViewById(R.id.et_product_key);
        etDeviceName = (EditText) findViewById(R.id.et_device_name);
        etDeviceSecret = (EditText) findViewById(R.id.et_device_secret);
        llDeviceInfo = (LinearLayout) findViewById(R.id.ll_device_info);
        etPropertyEventName = (EditText) findViewById(R.id.et_property_event_name);
        etValueContent = (EditText) findViewById(R.id.et_value_content);
        etVersion = (EditText) findViewById(R.id.et_version);

        llDeviceInfo.setVisibility(View.VISIBLE);
        hideUpgradeInfoUI();

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                              @Override
                                              //当AdapterView中的item被选中的时候执行的方法。
                                              public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                                                  if(null != jiotClientApi){
                                                      jiotClientApi.jiotSetLogLevel(position);
                                                  }
                                              }

                                              @Override
                                              //未选中时的时候执行的方法
                                              public void onNothingSelected(AdapterView<?> parent) {

                                              }
                                          });
        btnConnect.setOnClickListener(this);
        btnDisconnect.setOnClickListener(this);
        btnGetConnStatus.setOnClickListener(this);
        btnReportProperty.setOnClickListener(this);
        btnReportEvent.setOnClickListener(this);
        btnReportVersion.setOnClickListener(this);
        btnStartUpgrade.setOnClickListener(this);
        btnTest.setOnClickListener(this);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(null != jiotClientApi){
            jiotClientApi.jiotRelease();
        }
    }


    @Override
    public void onClick(View v) {

        switch (v.getId()){
            //连接服务器
            case R.id.btn_connect:
                doConnect();
                break;
            //断开与服务器的连接
            case R.id.btn_disconnect:
                doDisconnect();
                break;
            //获取客户端的连接状态
            case R.id.btn_get_conn_status:
                doGetConnStatus();
                break;
            //上报客户端的设备版本
            case R.id.btn_report_version:
                doReportVersion();
                break;
            //上报客户端的设备属性
            case R.id.btn_report_property:
                doReportProperty();
                break;
            //上报客户端的设备事件
            case R.id.btn_report_event:
                doReportEvent();
                break;
            //开始升级
            case R.id.btn_start_upgrade:
                doStartUpgrade();
                break;
            case R.id.btn_test:
                doTest();
                break;
            default:
                break;
        }
    }

    /**
     * 响应测试按钮操作
     */
    private void doTest() {
        if(jiotClientApi != null) {
            OtaStatusReportReq otaStatusReportReq = new OtaStatusReportReq();
            otaStatusReportReq.setDesc("upgrade failure,for test need.");
            otaStatusReportReq.setSeqNo(0);
            otaStatusReportReq.setStep(-1);
            otaStatusReportReq.setTaskId(otaTaskId);
            JiotResult res = jiotClientApi.jiotOtaStatusReportReq(otaStatusReportReq);
            if (res.getErrorCode() != 0) {
                showLogInfo("Client report ota process local error" + " errcode = " + res.getErrorCode() + ". \n");
            }
        }
    }

    /**
     * 响应按钮点击开始升级
     */
    private void doStartUpgrade() {

        if(otaUrl != null && otaMd5 != null) {
            DownloadUtils.downloadFirmware(otaUrl, STORAGE_PATH + "/" + otaMd5, otaMd5, new DownloadUtils.OtaDownloadCallback() {
                @Override
                public void onDownloadProgress(int percent) {
                    //下载进度，上报
                    OtaStatusReportReq otaStatusReportReq = new OtaStatusReportReq();
                    otaStatusReportReq.setDesc("current download progress");
                    otaStatusReportReq.setSeqNo(0);
                    otaStatusReportReq.setStep(percent);
                    otaStatusReportReq.setTaskId(otaTaskId);
                    JiotResult res = jiotClientApi.jiotOtaStatusReportReq(otaStatusReportReq);
                    if(res.getErrorCode() != 0){
                        showLogInfo("Client report ota process local error" + " errcode = " + res.getErrorCode() + ". \n");
                    }

                    Message message = mHandler.obtainMessage();
                    message.what = Constant.OTA_SHOW_PROGRESS;
                    message.arg1 = percent;
                    mHandler.sendMessage(message);
                }

                @Override
                public void onDownloadSuccess() {

                    OtaStatusReportReq otaStatusReportReq = new OtaStatusReportReq();
                    otaStatusReportReq.setDesc("download success.");
                    otaStatusReportReq.setSeqNo(0);
                    otaStatusReportReq.setStep(102);
                    otaStatusReportReq.setTaskId(otaTaskId);
                    JiotResult res = jiotClientApi.jiotOtaStatusReportReq(otaStatusReportReq);
                    if(res.getErrorCode() != 0){
                        showLogInfo("Client report ota process local error" + " errcode = " + res.getErrorCode() + ". \n");
                    }
                }

                @Override
                public void onDownloadFailure() {

                    OtaStatusReportReq otaStatusReportReq = new OtaStatusReportReq();
                    otaStatusReportReq.setDesc("download failure.");
                    otaStatusReportReq.setSeqNo(0);
                    otaStatusReportReq.setStep(-2);
                    otaStatusReportReq.setTaskId(otaTaskId);
                    JiotResult res = jiotClientApi.jiotOtaStatusReportReq(otaStatusReportReq);
                    if(res.getErrorCode() != 0){
                        showLogInfo("Client report ota process local error" + " errcode = " + res.getErrorCode() + ". \n");
                    }
                }

                @Override
                public void onFileMD5CheckSuccess(String outputFile) {

                    OtaStatusReportReq otaStatusReportReq = new OtaStatusReportReq();
                    otaStatusReportReq.setDesc("md5 check success.");
                    otaStatusReportReq.setSeqNo(0);
                    otaStatusReportReq.setStep(103);
                    otaStatusReportReq.setTaskId(otaTaskId);
                    JiotResult res = jiotClientApi.jiotOtaStatusReportReq(otaStatusReportReq);
                    if(res.getErrorCode() != 0){
                        showLogInfo("Client report ota process local error" + " errcode = " + res.getErrorCode() + ". \n");
                    }

                    Message message = mHandler.obtainMessage();
                    message.what = Constant.OTA_MD5_CHECK_SUCCESS;
                    message.obj = outputFile;
                    mHandler.sendMessage(message);
                }

                @Override
                public void onFileMD5CheckFailure() {
                    OtaStatusReportReq otaStatusReportReq = new OtaStatusReportReq();
                    otaStatusReportReq.setDesc("md5 check failure.");
                    otaStatusReportReq.setSeqNo(0);
                    otaStatusReportReq.setStep(-3);
                    otaStatusReportReq.setTaskId(otaTaskId);
                    JiotResult res = jiotClientApi.jiotOtaStatusReportReq(otaStatusReportReq);
                    if(res.getErrorCode() != 0){
                        showLogInfo("Client report ota process local error" + " errcode = " + res.getErrorCode() + ". \n");
                    }
                }
            });
        }


    }

    /**
     * 响应按钮点击上报设备事件
     */
    private void doReportEvent() {
        if(checkReportPropertyOrEventInput()) {

            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("propertyOrEventName", etPropertyEventName.getText().toString().trim());
            editor.commit();
            EventReportReq eventReportReq = new EventReportReq();
            eventReportReq.setSeqNo(0);
            Event event = new Event();
            event.setName(etPropertyEventName.getText().toString().trim());
            event.setContent(etValueContent.getText().toString().trim());
            event.setTime(System.currentTimeMillis());
            eventReportReq.setEvent(event);
            JiotResult res = jiotClientApi.jiotEventReportReq(eventReportReq);
            if(res.getErrorCode() != 0){
                showLogInfo("Client report event local error" + " errcode = " + res.getErrorCode() + ". \n");
            }
        }else{
            Toast.makeText(this,"Input Invalid",Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 响应按钮点击上报设备属性
     */
    private void doReportProperty() {
        if(checkReportPropertyOrEventInput()) {

            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("propertyOrEventName", etPropertyEventName.getText().toString().trim());
            editor.apply();

            PropertyReportReq propertyReportReq = new PropertyReportReq();
            propertyReportReq.setSeqNo(0);
            propertyReportReq.setVersion(1);
            //属性的数量，多属性测试时需要根据具体的值进行修改
            Property[] properties = new Property[1];
            Property property = new Property();
            property.setName(etPropertyEventName.getText().toString().trim());
            property.setTime(System.currentTimeMillis());
            property.setValue(etValueContent.getText().toString().trim());
            properties[0] = property;

            //测试多属性上报代码
            /**Property property1 = new Property();
            property1.setName(etPropertyEventName.getText().toString().trim() + "3");
            property1.setTime(System.currentTimeMillis());
            property1.setValue(etValueContent.getText().toString().trim());
            properties[1] = property1;**/
            propertyReportReq.setProperties(properties);
            JiotResult res = jiotClientApi.jiotPropertyReportReq(propertyReportReq);
            if(res.getErrorCode() != 0){
                showLogInfo("Client report property local error" + " errcode = " + res.getErrorCode() + ". \n");
            }
        }else{
            Toast.makeText(this,"Input Invalid",Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 校验上报设备属性或者事件输入数据的合法性
     * @return true 合法，false 非法
     */
    private boolean checkReportPropertyOrEventInput() {
        return !etPropertyEventName.getText().toString().trim().isEmpty() &&
                !etValueContent.getText().toString().trim().isEmpty();
    }

    /**
     * 响应按钮点击上报设备版本
     */
    private void doReportVersion() {

        if(checkReportVersionInput()) {
            VersionReportReq versionReportReq = new VersionReportReq();
            versionReportReq.setSeqNo(0);
            versionReportReq.setVersion(etVersion.getText().toString().trim());
            JiotResult res = jiotClientApi.jiotVersionReportReq(versionReportReq);
            if(res.getErrorCode() != 0){
                showLogInfo("Client report version local error" + " errcode = " + res.getErrorCode() + ". \n");
            }
        }else{
            Toast.makeText(this,"Input Invalid",Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 校验上报版本信息的输入数据合法性
     * @return true 合法，false 非法
     */
    private boolean checkReportVersionInput() {
        return !etVersion.getText().toString().trim().isEmpty();

    }

    /**
     * 响应按钮点击获取连接状态
     */
    private void doGetConnStatus() {
        int status = jiotClientApi.jiotGetConnStatus();

        Message message = mHandler.obtainMessage();
        message.what = Constant.GET_CONN_STATUS;
        message.arg1 = status;
        mHandler.sendMessage(message);
    }

    /**
     * 响应按钮点击断开连接
     */
    private void doDisconnect() {

        mHandler.sendEmptyMessage(Constant.DISCONNECTING);
        synchronized (JiotClientApi.class) {
            jiotClientApi.jiotDisConn();
        }
    }

    /**
     * 响应按钮点击连接
     */
    private void doConnect() {

        //检查输入合法性
        if(checkConnectInput()) {
            //屏蔽输入
            llDeviceInfo.setVisibility(View.GONE);
            mHandler.sendEmptyMessage(Constant.CONNECTING);

            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("productKey", etProductKey.getText().toString().trim());
            editor.putString("deviceName", etDeviceName.getText().toString().trim());
            editor.putString("deviceSecret", etDeviceSecret.getText().toString().trim());
            editor.apply();

            synchronized (JiotClientApi.class) {
                DeviceInfo deviceInfo = new DeviceInfo();
                deviceInfo.setProductKey(etProductKey.getText().toString().trim());
                deviceInfo.setDeviceName(etDeviceName.getText().toString().trim());
                deviceInfo.setDeviceSecret(etDeviceSecret.getText().toString().trim());
                int ret = jiotClientApi.jiotConn(deviceInfo, MainActivity.this, MainActivity.this);
                if (ret != 0) {
                    Message message = mHandler.obtainMessage();
                    message.arg1 = ret;
                    message.what = Constant.CONNECT_RESPONSE;
                    mHandler.sendMessage(message);
                }
            }
        }else{
            Toast.makeText(this,"Input Invalid",Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 检查输入连接设备信息的合法信息
     * @return true合法，false 非法
     */
    private boolean checkConnectInput() {
        return !etProductKey.getText().toString().trim().isEmpty() &&
                !etDeviceName.getText().toString().trim().isEmpty() &&
                !etDeviceSecret.getText().toString().trim().isEmpty();
    }


    /******************************连接状态回调************************************/
    @Override
    public void jiotConnectedHandle() {
        if(null != mHandler){
            mHandler.sendEmptyMessage(Constant.CONNECTED_HANDLE);
        }
    }

    @Override
    public void jiotConnectFailHandle(int errorCode) {
        if(null != mHandler){
            Message message = mHandler.obtainMessage();
            message.what = Constant.CONNECT_FAILE_HANDLE;
            message.arg1 = errorCode;
            mHandler.sendMessage(message);
        }
    }

    @Override
    public void jiotDisconnectHandle(int errorCode,String msg) {

        if(null != mHandler){
            Message message = mHandler.obtainMessage();
            message.what = Constant.DISCONNECT_HANDLE;
            message.arg1 = errorCode;
            message.obj = msg;
            mHandler.sendMessage(message);
        }
    }

    @Override
    public void jiotSubscribeFailHandle(String topicFilter) {
        if(null != mHandler){
            Message message = mHandler.obtainMessage();
            message.what = Constant.SUBSCRIBE_FAIL_HANDLE;
            message.obj = topicFilter;
            mHandler.sendMessage(message);
        }
    }

    @Override
    public void jiotPublishFailHandle(long seqNo) {
        if(null != mHandler){
            Message message = mHandler.obtainMessage();
            message.what = Constant.PUBLISH_FAIL_HANDLE;
            message.obj = seqNo;
            mHandler.sendMessage(message);
        }
    }

    @Override
    public void jiotMessageTimeoutHandle(long seqNo) {

        JiotLogger.e("jiotMessageTimeoutHandle callback jiotClientApi.jiotGetConnStatus() = " + jiotClientApi.jiotGetConnStatus());
        if(null != mHandler){
            Message message = mHandler.obtainMessage();
            message.what = Constant.MESSAGE_TIME_OUT_HANDLE;
            message.obj = seqNo;
            mHandler.sendMessage(message);
        }
    }


    /******************************消息接收回调************************************/
    @Override
    public void jiotPropertyReportRsp(PropertyReportRsp propertyReportRsp, int errorCode) {
        if(null != mHandler){
            Message message = mHandler.obtainMessage();
            message.obj = propertyReportRsp;
            message.arg1 = errorCode;
            message.what = Constant.REPORT_PROPERTY_RESPONSE;
            mHandler.sendMessage(message);
        }
    }

    @Override
    public void jiotEventReportRsp(EventReportRsp eventReportRsp, int errorCode) {
        if(null != mHandler){
            Message message = mHandler.obtainMessage();
            message.obj = eventReportRsp;
            message.arg1 = errorCode;
            message.what = Constant.REPORT_EVENT_RESPONSE;
            mHandler.sendMessage(message);
        }
    }

    @Override
    public void jiotVersionReportRsp(VersionReportRsp versionReportRsp, int errorCode) {
        if(null != mHandler){
            Message message = mHandler.obtainMessage();
            message.obj = versionReportRsp;
            message.arg1 = errorCode;
            message.what = Constant.REPORT_VERSION_RESPONSE;
            mHandler.sendMessage(message);
        }
    }

    @Override
    public void jiotPropertySetReq(PropertySetReq propertySetReq, int errorCode) {
        if(null != mHandler){
            Message message = mHandler.obtainMessage();
            message.obj = propertySetReq;
            message.arg1 = errorCode;
            message.what = Constant.PROPERTY_SET_REQ;
            mHandler.sendMessage(message);
        }
    }


    @Override
    public void jiotMsgDeliverReq(MsgDeliverReq msgDeliverReq, int errorCode) {
        if(null != mHandler){
            Message message = mHandler.obtainMessage();
            message.obj = msgDeliverReq;
            message.arg1 = errorCode;
            message.what = Constant.MSG_DELIVER_REQ;
            mHandler.sendMessage(message);
        }
    }

    @Override
    public void jiotOTAUpgradeInformReq(OtaUpgradeInformReq otaUpgradeInformReq, int errorCode) {
        if(null != mHandler){
            Message message = mHandler.obtainMessage();
            message.obj = otaUpgradeInformReq;
            message.arg1 = errorCode;
            message.what = Constant.OTA_UPGRADE_INFORM_REQ;
            mHandler.sendMessage(message);
        }
    }

    @Override
    public void jiotOTAStatusReportRsp(OtaStatusReportRsp otaStatusReportRsp, int errorCode) {
        if(null != mHandler){
            Message message = mHandler.obtainMessage();
            message.obj = otaStatusReportRsp;
            message.arg1 = errorCode;
            message.what = Constant.REPORT_OTA_STATUS_RESPONSE;
            mHandler.sendMessage(message);
        }
    }

    /**
     * 校验当前应用是否拥有安装未知来源app的权限
     * @param file
     */
    public void checkApp(File file) {

        if(file == null || !file.exists()){
            return;
        }
        boolean b = false;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            b = getPackageManager().canRequestPackageInstalls();
            if (b) {
                installApp(file);
            } else {
                showLogInfo( "请先在系统设置中的app下的打开安装未知应用权限，否则app将无法安装");
                Toast.makeText(MainActivity.this,"请先在系统设置中的app下的打开安装未知应用权限，否则app将无法安装",Toast.LENGTH_SHORT).show();
            }
        } else {
            installApp(file);
        }
    }

    /**
     * 调用系统功能安装apk
     * @param file
     */
    private void installApp(File file) {

        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(Intent.ACTION_VIEW);
        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uri = VersionFileProvider.getUriForFile(getApplicationContext(), getPackageName() + "" +
                    ".fileProvider", file);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            uri = Uri.fromFile(file);
        }
        intent.setDataAndType(uri,
                "application/vnd.android.package-archive");
        startActivity(intent);
    }
}

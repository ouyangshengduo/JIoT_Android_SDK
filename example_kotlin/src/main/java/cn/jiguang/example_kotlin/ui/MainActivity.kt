@file:Suppress("DEPRECATED_IDENTITY_EQUALS")

package cn.jiguang.example_kotlin.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Message
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import cn.jiguang.R

import java.io.File
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat

import cn.jiguang.example_kotlin.ui.base.BaseActivity
import cn.jiguang.example_kotlin.util.DownloadUtils
import cn.jiguang.iot.JiotClient
import cn.jiguang.iot.api.JiotClientApi
import cn.jiguang.iot.bean.DeviceInfo
import cn.jiguang.iot.bean.Event
import cn.jiguang.iot.bean.EventReportReq
import cn.jiguang.iot.bean.EventReportRsp
import cn.jiguang.iot.bean.MsgDeliverReq
import cn.jiguang.iot.bean.OtaStatusReportReq
import cn.jiguang.iot.bean.OtaStatusReportRsp
import cn.jiguang.iot.bean.OtaUpgradeInformReq
import cn.jiguang.iot.bean.Property
import cn.jiguang.iot.bean.PropertyReportReq
import cn.jiguang.iot.bean.PropertyReportRsp
import cn.jiguang.iot.bean.PropertySetReq
import cn.jiguang.iot.bean.VersionReportReq
import cn.jiguang.iot.bean.VersionReportRsp
import cn.jiguang.iot.callback.JclientHandleCallback
import cn.jiguang.iot.callback.JclientMessageCallback
import cn.jiguang.iot.util.JiotLogger
import kotlinx.android.synthetic.main.activity_main.*

/**
 * @author: ouyangshengduo
 * e-mail: ouysd@jiguang.cn
 * date  : 2019/4/9 10:29
 * desc  :
 */
class MainActivity : BaseActivity(), JclientHandleCallback, JclientMessageCallback, View.OnClickListener {

    private var jiotClientApi: JiotClientApi? = null

    private var preferences: SharedPreferences? = null
    private var localProductKey: String? = null
    private var localDeviceName: String? = null
    private var localDeviceSecret: String? = null
    private var localPropertyOrEventName: String? = null
    private var simpleDateFormat: SimpleDateFormat? = null

    private var otaUrl: String? = null
    private var otaMd5: String? = null
    private var otaTaskId: Long = 0

    private val mHandler = CallbackHandler(this)

    private class CallbackHandler
    /** 静态内部类构造方法
     * @param context 上下文
     */
    internal constructor(context: Context) : Handler() {
        private val reference: WeakReference<Context> = WeakReference(context)
        private var mainActivity: MainActivity? = null

        @SuppressLint("SetTextI18n")
        override fun handleMessage(msg: Message) {
            mainActivity = reference.get() as MainActivity
            if (mainActivity != null) {
                when (msg.what) {
                    Constant.CONNECTED_HANDLE -> mainActivity!!.showLogInfo("Client connected. \n")
                    Constant.CONNECT_FAILE_HANDLE -> {
                        mainActivity!!.showDisconnectStatusUI()
                        mainActivity!!.showLogInfo("Client connect fail,error code is " + msg.arg1 + ". \n")
                    }
                    Constant.GET_CONN_STATUS -> mainActivity!!.showLogInfo("Client connect status is " + Constant.CONN_STATUS[msg.arg1] + ". \n")
                    Constant.DISCONNECT_HANDLE -> {
                        mainActivity!!.showDisconnectStatusUI()
                        mainActivity!!.showLogInfo("Client disconnect " + " errcode = " + msg.arg1 + ". \n")
                    }
                    Constant.SUBSCRIBE_FAIL_HANDLE -> mainActivity!!.showLogInfo("Client subscribe fail and topic = " + msg.obj + ". \n")
                    Constant.MESSAGE_TIME_OUT_HANDLE -> mainActivity!!.showLogInfo("Client send message timeout and seq_no = " + msg.obj + ". \n")
                    Constant.PUBLISH_FAIL_HANDLE -> mainActivity!!.showLogInfo("Client publish fail and seq_no = " + msg.obj + ". \n")
                    Constant.REPORT_VERSION_RESPONSE -> {
                        val versionReportRsp = msg.obj as VersionReportRsp
                        mainActivity!!.showLogInfo("Client receive message (about report verion) from server" + " errcode = " + msg.arg1 + " code = " + versionReportRsp.code + " seq_no = " + versionReportRsp.seqNo + ". \n")
                    }
                    Constant.REPORT_PROPERTY_RESPONSE -> {
                        val propertyReportRsp = msg.obj as PropertyReportRsp
                        mainActivity!!.showLogInfo("Client receive message (about report property) from server" + " errcode = " + msg.arg1 + " code = " + propertyReportRsp.code + " seq_no = " + propertyReportRsp.seqNo + " version = " + propertyReportRsp.verion + " property_size = " + propertyReportRsp.properties.size + ". \n")
                    }
                    Constant.REPORT_EVENT_RESPONSE -> {
                        val eventReportRsp = msg.obj as EventReportRsp
                        mainActivity!!.showLogInfo("Client receive message (about report event) from server" + " errcode = " + msg.arg1 + " code = " + eventReportRsp.code + " seq_no = " + eventReportRsp.seqNo + ". \n")
                    }
                    Constant.MSG_DELIVER_REQ -> {
                        val msgDeliverReq = msg.obj as MsgDeliverReq
                        mainActivity!!.showLogInfo("Client receive message (about msg deliver) from server" + " errcode = " + msg.arg1 + " seq_no = " + msgDeliverReq.seqNo + " message = " + msgDeliverReq.message + " timestamp = " + msgDeliverReq.time + ". \n")
                    }
                    Constant.PROPERTY_SET_REQ -> {
                        val propertySetReq = msg.obj as PropertySetReq
                        mainActivity!!.showLogInfo("Client receive message (about set property) from server" + " errcode = " + msg.arg1 + " seq_no = " + propertySetReq.seqNo + " verion = " + propertySetReq.version + " property_size = " + propertySetReq.properties.size + ". \n")
                    }
                    Constant.CONNECTING -> mainActivity!!.showLogInfo("Client connecting... " + " \n")
                    Constant.DISCONNECTING -> mainActivity!!.showLogInfo("Client disconnecting... " + " \n")
                    Constant.CONNECT_RESPONSE -> {
                        mainActivity!!.showDisconnectStatusUI()
                        mainActivity!!.showLogInfo("Client connect response code = " + msg.arg1 + " \n")
                    }
                    Constant.OTA_UPGRADE_INFORM_REQ -> {
                        mainActivity!!.showUpgradeInfoUI()
                        val otaUpgradeInformReq = msg.obj as OtaUpgradeInformReq
                        mainActivity!!.otaUrl = otaUpgradeInformReq.data.url
                        mainActivity!!.otaMd5 = otaUpgradeInformReq.data.md5
                        mainActivity!!.otaTaskId = otaUpgradeInformReq.data.taskId
                        mainActivity!!.showLogInfo("Client receive message (about ota upgrade inform) from server" + " errcode = " + msg.arg1 + " seq_no = " + otaUpgradeInformReq.seqNo + " data = " + otaUpgradeInformReq.data.toString() + ". \n")
                    }
                    Constant.OTA_SHOW_PROGRESS -> mainActivity!!.tvUpgradeProcess!!.text = msg.arg1.toString() + "%"
                    Constant.REPORT_OTA_STATUS_RESPONSE -> {
                        val otaStatusReportRsp = msg.obj as OtaStatusReportRsp
                        mainActivity!!.showLogInfo("Client receive message (about report ota status) from server" + " errcode = " + msg.arg1 + " code = " + otaStatusReportRsp.code + " seq_no = " + otaStatusReportRsp.seqNo + ". \n")
                    }
                    Constant.OTA_MD5_CHECK_SUCCESS -> {
                        val outputFile = msg.obj as String
                        mainActivity!!.checkApp(File(outputFile))
                    }
                    else -> {
                    }
                }
            }
        }


    }

    /**
     * 当收到升级请求后UI的模拟处理
     */
    private fun showUpgradeInfoUI() {
        if (llUpgradeInfo != null) {
            llUpgradeInfo!!.visibility = View.VISIBLE
        }
    }

    /**
     * 当升级完成，隐藏布局
     */
    private fun hideUpgradeInfoUI() {
        if (llUpgradeInfo != null) {
            llUpgradeInfo!!.visibility = View.GONE
        }
    }

    /**
     * 显示日志
     * @param content 日志内容
     */
    private fun showLogInfo(content: String) {
        val currentTime = System.currentTimeMillis()
        val timeNow = simpleDateFormat!!.format(currentTime)
        tvLogInfo!!.append("$timeNow:  $content")
    }

    /**
     * 设备连接失败的UI变化
     */
    private fun showDisconnectStatusUI() {
        llDeviceInfo!!.visibility = View.VISIBLE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT !== 0) {
            finish()
            return
        }
        setContentView(R.layout.activity_main)

        initView()
        initData()
        requestPermission()
    }


    private fun requestPermission() {
        //检查权限是否存在
        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            //向用户申请授权
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE), 1)

        }
    }

    /**
     * 数据对象的初始化
     */
    @SuppressLint("SimpleDateFormat")
    private fun initData() {
        preferences = getSharedPreferences(DEVICE_INFO, Activity.MODE_PRIVATE)
        localProductKey = preferences!!.getString("productKey", null)
        localDeviceName = preferences!!.getString("deviceName", null)
        localDeviceSecret = preferences!!.getString("deviceSecret", null)
        localPropertyOrEventName = preferences!!.getString("propertyOrEventName", null)
        jiotClientApi = JiotClient.getInstance()
        jiotClientApi!!.jiotInit(this, false)
        etProductKey!!.setText(if (localProductKey == null) Constant.DEFAULT_PRODUCT_KEY else localProductKey)
        etDeviceName!!.setText(if (localDeviceName == null) Constant.DEFAULT_DEVICE_NAME else localDeviceName)
        etDeviceSecret!!.setText(if (localDeviceSecret == null) Constant.DEFAULT_DEVICE_SECRET else localDeviceSecret)
        etVersion!!.setText(Constant.DEFAULT_REPORT_VERSION)
        etPropertyEventName!!.setText(if (localPropertyOrEventName == null) Constant.DEFAULT_DEVICE_PROPERTY else localPropertyOrEventName)
        etValueContent!!.setText(Constant.DEFAULT_REPORT_CONTENT)

        simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    }

    /**
     * 界面UI控件的初始化
     */
    private fun initView() {

        llDeviceInfo!!.visibility = View.VISIBLE
        hideUpgradeInfoUI()

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override//当AdapterView中的item被选中的时候执行的方法。
            fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {

                if (null != jiotClientApi) {
                    jiotClientApi!!.jiotSetLogLevel(position)
                }
            }

            override//未选中时的时候执行的方法
            fun onNothingSelected(parent: AdapterView<*>) {

            }
        }
        btnConnect.setOnClickListener(this)
        btnDisconnect.setOnClickListener(this)
        btnGetConnStatus.setOnClickListener(this)
        btnReportProperty.setOnClickListener(this)
        btnReportEvent.setOnClickListener(this)
        btnReportVersion.setOnClickListener(this)
        btnStartUpgrade.setOnClickListener(this)
        //btnTest.setOnClickListener(this)

    }

    override fun onDestroy() {
        super.onDestroy()
        if (null != jiotClientApi) {
            jiotClientApi!!.jiotRelease()
        }
    }


    override fun onClick(v: View) {

        when (v.id) {
            //连接服务器
            R.id.btnConnect -> doConnect()
            //断开与服务器的连接
            R.id.btnDisconnect -> doDisconnect()
            //获取客户端的连接状态
            R.id.btnGetConnStatus -> doGetConnStatus()
            //上报客户端的设备版本
            R.id.btnReportVersion -> doReportVersion()
            //上报客户端的设备属性
            R.id.btnReportProperty -> doReportProperty()
            //上报客户端的设备事件
            R.id.btnReportEvent -> doReportEvent()
            //开始升级
            R.id.btnStartUpgrade -> doStartUpgrade()
            else -> {
            }
        }
    }


    /**
     * 响应按钮点击开始升级
     */
    private fun doStartUpgrade() {

        if (otaUrl != null && otaMd5 != null) {
            DownloadUtils.downloadFirmware(otaUrl!!, "$STORAGE_PATH/$otaMd5", otaMd5!!, object : DownloadUtils.OtaDownloadCallback {
                override fun onDownloadProgress(percent: Int) {
                    //下载进度，上报
                    val otaStatusReportReq = OtaStatusReportReq()
                    otaStatusReportReq.desc = "current download progress"
                    otaStatusReportReq.seqNo = 0
                    otaStatusReportReq.step = percent
                    otaStatusReportReq.taskId = otaTaskId
                    val res = jiotClientApi!!.jiotOtaStatusReportReq(otaStatusReportReq)
                    if (res.errorCode != 0) {
                        showLogInfo("Client report ota process local error" + " errcode = " + res.errorCode + ". \n")
                    }

                    val message = mHandler.obtainMessage()
                    message.what = Constant.OTA_SHOW_PROGRESS
                    message.arg1 = percent
                    mHandler.sendMessage(message)
                }

                override fun onDownloadSuccess() {

                    val otaStatusReportReq = OtaStatusReportReq()
                    otaStatusReportReq.desc = "download success."
                    otaStatusReportReq.seqNo = 0
                    otaStatusReportReq.step = 102
                    otaStatusReportReq.taskId = otaTaskId
                    val res = jiotClientApi!!.jiotOtaStatusReportReq(otaStatusReportReq)
                    if (res.errorCode != 0) {
                        showLogInfo("Client report ota process local error" + " errcode = " + res.errorCode + ". \n")
                    }
                }

                override fun onDownloadFailure() {

                    val otaStatusReportReq = OtaStatusReportReq()
                    otaStatusReportReq.desc = "download failure."
                    otaStatusReportReq.seqNo = 0
                    otaStatusReportReq.step = -2
                    otaStatusReportReq.taskId = otaTaskId
                    val res = jiotClientApi!!.jiotOtaStatusReportReq(otaStatusReportReq)
                    if (res.errorCode != 0) {
                        showLogInfo("Client report ota process local error" + " errcode = " + res.errorCode + ". \n")
                    }
                }

                override fun onFileMD5CheckSuccess(outputFile: String) {

                    val otaStatusReportReq = OtaStatusReportReq()
                    otaStatusReportReq.desc = "md5 check success."
                    otaStatusReportReq.seqNo = 0
                    otaStatusReportReq.step = 103
                    otaStatusReportReq.taskId = otaTaskId
                    val res = jiotClientApi!!.jiotOtaStatusReportReq(otaStatusReportReq)
                    if (res.errorCode != 0) {
                        showLogInfo("Client report ota process local error" + " errcode = " + res.errorCode + ". \n")
                    }

                    val message = mHandler.obtainMessage()
                    message.what = Constant.OTA_MD5_CHECK_SUCCESS
                    message.obj = outputFile
                    mHandler.sendMessage(message)
                }

                override fun onFileMD5CheckFailure() {
                    val otaStatusReportReq = OtaStatusReportReq()
                    otaStatusReportReq.desc = "md5 check failure."
                    otaStatusReportReq.seqNo = 0
                    otaStatusReportReq.step = -3
                    otaStatusReportReq.taskId = otaTaskId
                    val res = jiotClientApi!!.jiotOtaStatusReportReq(otaStatusReportReq)
                    if (res.errorCode != 0) {
                        showLogInfo("Client report ota process local error" + " errcode = " + res.errorCode + ". \n")
                    }
                }
            })
        }


    }

    /**
     * 响应按钮点击上报设备事件
     */
    private fun doReportEvent() {
        if (checkReportPropertyOrEventInput()) {

            val editor = preferences!!.edit()
            editor.putString("propertyOrEventName", etPropertyEventName!!.text.toString().trim { it <= ' ' })
            editor.apply()
            val eventReportReq = EventReportReq()
            eventReportReq.seqNo = 0
            val event = Event()
            event.name = etPropertyEventName!!.text.toString().trim { it <= ' ' }
            event.content = etValueContent!!.text.toString().trim { it <= ' ' }
            event.time = System.currentTimeMillis()
            eventReportReq.event = event
            val res = jiotClientApi!!.jiotEventReportReq(eventReportReq)
            if (res.errorCode != 0) {
                showLogInfo("Client report event local error" + " errcode = " + res.errorCode + ". \n")
            }
        } else {
            Toast.makeText(this, "Input Invalid", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 响应按钮点击上报设备属性
     */
    private fun doReportProperty() {
        if (checkReportPropertyOrEventInput()) {

            val editor = preferences!!.edit()
            editor.putString("propertyOrEventName", etPropertyEventName!!.text.toString().trim { it <= ' ' })
            editor.apply()

            val propertyReportReq = PropertyReportReq()
            propertyReportReq.seqNo = 0
            propertyReportReq.version = 1
            //属性的数量，多属性测试时需要根据具体的值进行修改
            val properties = arrayOfNulls<Property>(1)
            val property = Property()
            property.name = etPropertyEventName!!.text.toString().trim { it <= ' ' }
            property.time = System.currentTimeMillis()
            property.value = etValueContent!!.text.toString().trim { it <= ' ' }
            properties[0] = property

            //测试多属性上报代码
            /**Property property1 = new Property();
             * property1.setName(etPropertyEventName.getText().toString().trim() + "3");
             * property1.setTime(System.currentTimeMillis());
             * property1.setValue(etValueContent.getText().toString().trim());
             * properties[1] = property1; */
            propertyReportReq.properties = properties
            val res = jiotClientApi!!.jiotPropertyReportReq(propertyReportReq)
            if (res.errorCode != 0) {
                showLogInfo("Client report property local error" + " errcode = " + res.errorCode + ". \n")
            }
        } else {
            Toast.makeText(this, "Input Invalid", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 校验上报设备属性或者事件输入数据的合法性
     * @return true 合法，false 非法
     */
    private fun checkReportPropertyOrEventInput(): Boolean {
        return !etPropertyEventName!!.text.toString().trim { it <= ' ' }.isEmpty() && !etValueContent!!.text.toString().trim { it <= ' ' }.isEmpty()
    }

    /**
     * 响应按钮点击上报设备版本
     */
    private fun doReportVersion() {

        if (checkReportVersionInput()) {
            val versionReportReq = VersionReportReq()
            versionReportReq.seqNo = 0
            versionReportReq.version = etVersion!!.text.toString().trim { it <= ' ' }
            val res = jiotClientApi!!.jiotVersionReportReq(versionReportReq)
            if (res.errorCode != 0) {
                showLogInfo("Client report version local error" + " errcode = " + res.errorCode + ". \n")
            }
        } else {
            Toast.makeText(this, "Input Invalid", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 校验上报版本信息的输入数据合法性
     * @return true 合法，false 非法
     */
    private fun checkReportVersionInput(): Boolean {
        return !etVersion!!.text.toString().trim { it <= ' ' }.isEmpty()

    }

    /**
     * 响应按钮点击获取连接状态
     */
    private fun doGetConnStatus() {
        val status = jiotClientApi!!.jiotGetConnStatus()

        val message = mHandler.obtainMessage()
        message.what = Constant.GET_CONN_STATUS
        message.arg1 = status
        mHandler.sendMessage(message)
    }

    /**
     * 响应按钮点击断开连接
     */
    private fun doDisconnect() {

        mHandler.sendEmptyMessage(Constant.DISCONNECTING)
        synchronized(JiotClientApi::class.java) {
            jiotClientApi!!.jiotDisConn()
        }
    }

    /**
     * 响应按钮点击连接
     */
    private fun doConnect() {

        //检查输入合法性
        if (checkConnectInput()) {
            //屏蔽输入
            llDeviceInfo!!.visibility = View.GONE
            mHandler.sendEmptyMessage(Constant.CONNECTING)

            val editor = preferences!!.edit()
            editor.putString("productKey", etProductKey!!.text.toString().trim { it <= ' ' })
            editor.putString("deviceName", etDeviceName!!.text.toString().trim { it <= ' ' })
            editor.putString("deviceSecret", etDeviceSecret!!.text.toString().trim { it <= ' ' })
            editor.apply()

            synchronized(JiotClientApi::class.java) {
                val deviceInfo = DeviceInfo()
                deviceInfo.productKey = etProductKey!!.text.toString().trim { it <= ' ' }
                deviceInfo.deviceName = etDeviceName!!.text.toString().trim { it <= ' ' }
                deviceInfo.deviceSecret = etDeviceSecret!!.text.toString().trim { it <= ' ' }
                val ret = jiotClientApi!!.jiotConn(deviceInfo, this@MainActivity, this@MainActivity)
                if (ret != 0) {
                    val message = mHandler.obtainMessage()
                    message.arg1 = ret
                    message.what = Constant.CONNECT_RESPONSE
                    mHandler.sendMessage(message)
                }
            }
        } else {
            Toast.makeText(this, "Input Invalid", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 检查输入连接设备信息的合法信息
     * @return true合法，false 非法
     */
    private fun checkConnectInput(): Boolean {
        return !etProductKey!!.text.toString().trim { it <= ' ' }.isEmpty() &&
                !etDeviceName!!.text.toString().trim { it <= ' ' }.isEmpty() &&
                !etDeviceSecret!!.text.toString().trim { it <= ' ' }.isEmpty()
    }


    /******************************连接状态回调 */
    override fun jiotConnectedHandle() {
        mHandler.sendEmptyMessage(Constant.CONNECTED_HANDLE)
    }

    override fun jiotConnectFailHandle(errorCode: Int) {
        val message = mHandler.obtainMessage()
        message.what = Constant.CONNECT_FAILE_HANDLE
        message.arg1 = errorCode
        mHandler.sendMessage(message)
    }

    override fun jiotDisconnectHandle(errorCode: Int, msg: String) {

        val message = mHandler.obtainMessage()
        message.what = Constant.DISCONNECT_HANDLE
        message.arg1 = errorCode
        message.obj = msg
        mHandler.sendMessage(message)
    }

    override fun jiotSubscribeFailHandle(topicFilter: String) {
        val message = mHandler.obtainMessage()
        message.what = Constant.SUBSCRIBE_FAIL_HANDLE
        message.obj = topicFilter
        mHandler.sendMessage(message)
    }

    override fun jiotPublishFailHandle(seqNo: Long) {
        val message = mHandler.obtainMessage()
        message.what = Constant.PUBLISH_FAIL_HANDLE
        message.obj = seqNo
        mHandler.sendMessage(message)
    }

    override fun jiotMessageTimeoutHandle(seqNo: Long) {

        JiotLogger.e("jiotMessageTimeoutHandle callback jiotClientApi.jiotGetConnStatus() = " + jiotClientApi!!.jiotGetConnStatus())
        val message = mHandler.obtainMessage()
        message.what = Constant.MESSAGE_TIME_OUT_HANDLE
        message.obj = seqNo
        mHandler.sendMessage(message)
    }


    /******************************消息接收回调 */
    override fun jiotPropertyReportRsp(propertyReportRsp: PropertyReportRsp, errorCode: Int) {
        val message = mHandler.obtainMessage()
        message.obj = propertyReportRsp
        message.arg1 = errorCode
        message.what = Constant.REPORT_PROPERTY_RESPONSE
        mHandler.sendMessage(message)
    }

    override fun jiotEventReportRsp(eventReportRsp: EventReportRsp, errorCode: Int) {
        val message = mHandler.obtainMessage()
        message.obj = eventReportRsp
        message.arg1 = errorCode
        message.what = Constant.REPORT_EVENT_RESPONSE
        mHandler.sendMessage(message)
    }

    override fun jiotVersionReportRsp(versionReportRsp: VersionReportRsp, errorCode: Int) {
        val message = mHandler.obtainMessage()
        message.obj = versionReportRsp
        message.arg1 = errorCode
        message.what = Constant.REPORT_VERSION_RESPONSE
        mHandler.sendMessage(message)
    }

    override fun jiotPropertySetReq(propertySetReq: PropertySetReq, errorCode: Int) {
        val message = mHandler.obtainMessage()
        message.obj = propertySetReq
        message.arg1 = errorCode
        message.what = Constant.PROPERTY_SET_REQ
        mHandler.sendMessage(message)
    }


    override fun jiotMsgDeliverReq(msgDeliverReq: MsgDeliverReq, errorCode: Int) {
        val message = mHandler.obtainMessage()
        message.obj = msgDeliverReq
        message.arg1 = errorCode
        message.what = Constant.MSG_DELIVER_REQ
        mHandler.sendMessage(message)
    }

    override fun jiotOTAUpgradeInformReq(otaUpgradeInformReq: OtaUpgradeInformReq, errorCode: Int) {
        val message = mHandler.obtainMessage()
        message.obj = otaUpgradeInformReq
        message.arg1 = errorCode
        message.what = Constant.OTA_UPGRADE_INFORM_REQ
        mHandler.sendMessage(message)
    }

    override fun jiotOTAStatusReportRsp(otaStatusReportRsp: OtaStatusReportRsp, errorCode: Int) {
        val message = mHandler.obtainMessage()
        message.obj = otaStatusReportRsp
        message.arg1 = errorCode
        message.what = Constant.REPORT_OTA_STATUS_RESPONSE
        mHandler.sendMessage(message)
    }

    /**
     * 校验当前应用是否拥有安装未知来源app的权限
     * @param file
     */
    fun checkApp(file: File?) {

        if (file == null || !file.exists()) {
            return
        }
        val b : Boolean
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            b = packageManager.canRequestPackageInstalls()
            if (b) {
                installApp(file)
            } else {
                showLogInfo("请先在系统设置中的app下的打开安装未知应用权限，否则app将无法安装")
                Toast.makeText(this@MainActivity, "请先在系统设置中的app下的打开安装未知应用权限，否则app将无法安装", Toast.LENGTH_SHORT).show()
            }
        } else {
            installApp(file)
        }
    }

    /**
     * 调用系统功能安装apk
     * @param file
     */
    private fun installApp(file: File) {

        val intent = Intent()
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.action = Intent.ACTION_VIEW
        val uri: Uri
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uri = FileProvider.getUriForFile(applicationContext, packageName + "" +
                    ".fileProvider", file)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            uri = Uri.fromFile(file)
        }
        intent.setDataAndType(uri,
                "application/vnd.android.package-archive")
        startActivity(intent)
    }

    companion object {
        private const val DEVICE_INFO = "DEVICE_INFO"
        private val STORAGE_PATH = Environment.getExternalStorageDirectory().absolutePath
    }
}

package cn.jiguang.example_kotlin.ui

/**
 * @author : ouyangshengduo
 * e-mail : ouysd@jiguang.cn
 * date  : 2019/4/16 13:35
 * desc :
 */
internal object Constant {

    /**
     * 连接状态枚举值
     */
    val CONN_STATUS = arrayOf("NONE", "CLIENT_INITIALIZED", "CLIENT_CONNECTING", "CLIENT_CONNECTED", "CLIENT_DISCONNECTING", "CLIENT_DISCONNECTED", "CLIENT_RECONNECTING")
    /**
     * 连接成功处理
     */
    const val CONNECTED_HANDLE = 1000
    /**
     * 连接失败处理
     */
    const val CONNECT_FAILE_HANDLE = 1001
    /**
     * 获取连接状态处理
     */
    const val GET_CONN_STATUS = 1002
    /**
     * 客户端收到消息后的处理
     */
    const val MSG_DELIVER_REQ = 1003
    /**
     * 客户端端口处理
     */
    const val DISCONNECT_HANDLE = 1004
    /**
     * 订阅失败处理
     */
    const val SUBSCRIBE_FAIL_HANDLE = 1005
    /**
     * 消息超时处理
     */
    const val MESSAGE_TIME_OUT_HANDLE = 1006
    /**
     * 发布失败处理
     */
    const val PUBLISH_FAIL_HANDLE = 1007
    /**
     * 上报版本后回复的处理
     */
    const val REPORT_VERSION_RESPONSE = 1008
    /**
     * 上报属性后回复的处理
     */
    const val REPORT_PROPERTY_RESPONSE = 1009
    /**
     * 上报事件回复的处理
     */
    const val REPORT_EVENT_RESPONSE = 1010
    /**
     * 客户端收到设置设备属性的处理
     */
    const val PROPERTY_SET_REQ = 1011
    /**
     * 客户端连接中
     */
    const val CONNECTING = 1013
    /**
     * 客户端断开连接中
     */
    const val DISCONNECTING = 1014
    /**
     * 实时连接返回状态处理
     */
    const val CONNECT_RESPONSE = 1015

    /**
     * 客户端收到升级请求的处理
     */
    const val OTA_UPGRADE_INFORM_REQ = 1016

    /**
     * 上报OTA升级状态后的回复的处理
     */
    const val REPORT_OTA_STATUS_RESPONSE = 1017

    /**
     * OTA升级进度显示
     */
    const val OTA_SHOW_PROGRESS = 1018

    const val OTA_MD5_CHECK_SUCCESS = 1019

    /**
     * 默认产品key
     */
    const val DEFAULT_PRODUCT_KEY = ""

    /**
     * 默认产品名称
     */
    const val DEFAULT_DEVICE_NAME = ""
    /**
     * 默认设备密钥
     */
    const val DEFAULT_DEVICE_SECRET = ""
    /**
     * 默认设备属性
     */
    const val DEFAULT_DEVICE_PROPERTY = ""
    /**
     * 设备上传的内容
     */
    const val DEFAULT_REPORT_CONTENT = "jiguang test content"

    const val DEFAULT_REPORT_VERSION = "1.0.3"
}

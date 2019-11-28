package cn.jiguang.example_kotlin.util

import android.annotation.SuppressLint

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.ExecutorService
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

import cn.jiguang.iot.util.JiotLogger

/**
 * @author : ouyangshengduo
 * e-mail : ouysd@jiguang.cn
 * date  : 2019/7/18 17:48
 * desc : OTA下载工具
 */
object DownloadUtils {
    /**http 默认连接时间 单位:毫秒 */
    private const val DEFAULT_CONNECT_TIMEOUT = 10000
    /**http 默认读取时间 单位:毫秒 */
    private const val DEFAULT_READ_TIMEOUT = 10000
    /**默认重试3次 */
    private const val MAX_TRY_TIMES = 3
    /**下载时接收buff最大字节 */
    private const val BUFFER_MAX_SIZE = 1024
    /**区别https的开头字符串 */
    private const val HTTPS_URL_START = "https://"
    /**声明一个线程池 */
    private var executorService: ExecutorService? = null

    @Volatile
    private var downloadThreadRunning = false

    interface OtaDownloadCallback {
        /**
         * OTA升级包下载进度回调
         * @param percent  下载进度（0 ~ 100）
         */
        fun onDownloadProgress(percent: Int)

        /**
         * OTA升级包下载完成回调
         */
        fun onDownloadSuccess()

        /**
         * OTA升级包下载失败回调
         */
        fun onDownloadFailure()

        /**
         * OTA升级包MD5校验成功
         * @param outputFile  已下载完成的升级包文件名（包含全路径）
         */
        fun onFileMD5CheckSuccess(outputFile: String)

        /**
         * OTA升级包MD5校验失败
         */
        fun onFileMD5CheckFailure()
    }

    /**
     * 线程池的初始化
     * @return 返回线程池
     */
    @Synchronized
    private fun initExecutorService(): ExecutorService {

        if (null == executorService) {
            //这里只是给这个线程起一个名字
            val threadFactory = ThreadFactory { runnable -> Thread(runnable, "OTA Download Thread") }
            //这里按照OkHttp的线程池样式来创建，单个线程在闲置的时候保留60秒
            executorService = ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, SynchronousQueue(), threadFactory)
        }
        return executorService as ExecutorService
    }


    /**
     * 开启线程下载固件
     * @param firmwareURL 固件URL
     * @param outputFile  固件要保存的本地全路径及文件名
     * @param md5Sum      用于下载完成后做校验的MD5
     * @param otaDownloadCallback  过程回调
     */
    fun downloadFirmware(firmwareURL: String, outputFile: String, md5Sum: String, otaDownloadCallback: OtaDownloadCallback) {
        initExecutorService().execute(Runnable {
            if (downloadThreadRunning) {
                JiotLogger.d("ota download thread is running")
                return@Runnable
            }
            downloadThreadRunning = true
            var tryTimes = 0
            do {
                var fos: RandomAccessFile? = null
                var stream: InputStream? = null
                try {
                    tryTimes++
                    fos = RandomAccessFile(outputFile, "rw")
                    var downloadBytes: Long = 0
                    var lastPercent = 0

                    //建立http连接
                    JiotLogger.d("connecting : $firmwareURL")
                    val conn = createURLConnection(firmwareURL)
                    conn.connectTimeout = DEFAULT_CONNECT_TIMEOUT
                    conn.readTimeout = DEFAULT_READ_TIMEOUT
                    conn.setRequestProperty("Range", "bytes=$downloadBytes-")
                    conn.connect()

                    val totalLength = conn.contentLength
                    JiotLogger.d("totalLength $totalLength bytes")

                    stream = conn.inputStream
                    val buffer = ByteArray(BUFFER_MAX_SIZE)
                    while (downloadBytes < totalLength) {
                        val len = stream!!.read(buffer)
                        if (len < 0) {
                            break
                        }
                        downloadBytes += len.toLong()
                        fos.write(buffer, 0, len)
                        //计算下载进度
                        val percent = (downloadBytes.toFloat() / totalLength.toFloat() * 100).toInt()
                        if (percent != lastPercent) {
                            lastPercent = percent
                            //回调到外界显示下载进度
                            otaDownloadCallback.onDownloadProgress(percent)
                            JiotLogger.d("download $downloadBytes bytes. percent:$percent")
                        }
                    }
                    //下载完成，关闭资源
                    fos.close()
                    stream?.close()
                    //回调下载成功
                    otaDownloadCallback.onDownloadSuccess()
                    //计算文件的md5值
                    val calcMD5 = fileToMD5(outputFile)
                    if (!calcMD5.equals(md5Sum, ignoreCase = true)) {
                        JiotLogger.d("server md5 string is $md5Sum")
                        JiotLogger.e("md5 checksum not match!!! calculated md5:$calcMD5")
                        //回调md5校验失败
                        otaDownloadCallback.onFileMD5CheckFailure()
                        //删除文件
                        val deleteResule = File(outputFile).delete()
                        if (!deleteResule) {
                            JiotLogger.d("delete file error.")
                        }
                        break
                    } else {
                        //回调md5校验成功
                        otaDownloadCallback.onFileMD5CheckSuccess(outputFile)
                        break
                    }
                } catch (e: CertificateException) {
                    otaDownloadCallback.onFileMD5CheckFailure()
                } catch (e: Exception) {
                    e.printStackTrace()
                    otaDownloadCallback.onDownloadFailure()
                } finally {
                    if (fos != null) {
                        try {
                            fos.close()
                        } catch (ignored: Exception) {
                        }

                    }
                    if (stream != null) {
                        try {
                            stream.close()
                        } catch (ignored: Exception) {
                        }

                    }
                }
            } while (tryTimes <= MAX_TRY_TIMES)

            downloadThreadRunning = false
        })
    }


    /**
     * 根据URL创建对应的HTTP或HTTPS连接对象
     * @param firmwareURL 固件URL
     * @return HttpURLConnection或HttpsURLConnection对象
     */
    @Throws(Exception::class)
    private fun createURLConnection(firmwareURL: String): HttpURLConnection {

        if (firmwareURL.toLowerCase().startsWith(HTTPS_URL_START)) {
            val url = URL(firmwareURL)
            val conn = url.openConnection() as HttpsURLConnection
            val sslContext = SSLContext.getInstance("SSL")
            val tm = arrayOf<TrustManager>(object : X509TrustManager {
                @SuppressLint("TrustAllX509TrustManager")
                override fun checkClientTrusted(x509Certificates: Array<X509Certificate>, s: String) {
                }

                @SuppressLint("TrustAllX509TrustManager")
                override fun checkServerTrusted(x509Certificates: Array<X509Certificate>, s: String) {
                }

                override fun getAcceptedIssuers(): Array<X509Certificate>? {
                    return null
                }
            })
            sslContext.init(null, tm, java.security.SecureRandom())
            val ssf = sslContext.socketFactory
            conn.sslSocketFactory = ssf
            return conn
        }
        val url = URL(firmwareURL)
        return url.openConnection() as HttpURLConnection
    }

    /**
     * 计算文件的MD5摘要值
     * @param filePath 全路径文件名
     * @return 以16进制字符表示的摘要字符串
     */
    private fun fileToMD5(filePath: String): String {
        var inputStream: InputStream? = null
        try {
            inputStream = FileInputStream(filePath)
            val buffer = ByteArray(BUFFER_MAX_SIZE)
            val digest = MessageDigest.getInstance("MD5")
            var numRead = 0
            while (numRead != -1) {
                numRead = inputStream.read(buffer)
                if (numRead > 0) {
                    digest.update(buffer, 0, numRead)
                }
            }
            val md5Bytes = digest.digest()
            return convertHashToString(md5Bytes)
        } catch (e: Exception) {
            return ""
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close()
                } catch (ignored: Exception) {
                }

            }
        }
    }

    /**
     * 转换摘要值为字符串形式
     * @param digestBytes 二进制摘要值
     * @return 以16进制字符表示的摘要字符串
     */
    private fun convertHashToString(digestBytes: ByteArray): String {
        return with(StringBuilder()) {
            digestBytes.forEach {
                val hex = it.toInt() and (0xFF)
                val hexStr = Integer.toHexString(hex)
                if (hexStr.length == 1) {
                    this.append("0").append(hexStr)
                } else {
                    this.append(hexStr)
                }
            }
            this.toString()
        }
    }

}

package cn.jiguang.example.util;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import cn.jiguang.iot.util.JiotLogger;
/**
 * @author : ouyangshengduo
 * e-mail : ouysd@jiguang.cn
 * date  : 2019/7/18 17:48
 * desc : OTA下载工具
 */
public class DownloadUtils {
    /**http 默认连接时间 单位:毫秒**/
    private static final int DEFAULT_CONNECT_TIMEOUT = 10000;
    /**http 默认读取时间 单位:毫秒**/
    private static final int DEFAULT_READ_TIMEOUT    = 10000;
    /**默认重试3次**/
    private static final int MAX_TRY_TIMES = 3;
    /**下载时接收buff最大字节**/
    private static final int BUFFER_MAX_SIZE = 1024;
    /**区别https的开头字符串**/
    private static final String HTTPS_URL_START = "https://";
    /**声明一个线程池**/
    private static ExecutorService executorService;

    private static volatile boolean downloadThreadRunning = false;

    public interface OtaDownloadCallback {
        /**
         * OTA升级包下载进度回调
         * @param percent  下载进度（0 ~ 100）
         */
        void onDownloadProgress(int percent);

        /**
         * OTA升级包下载完成回调
         */
        void onDownloadSuccess();

        /**
         * OTA升级包下载失败回调
         */
        void onDownloadFailure();

        /**
         * OTA升级包MD5校验成功
         * @param outputFile  已下载完成的升级包文件名（包含全路径）
         */
        void onFileMD5CheckSuccess(String outputFile);

        /**
         * OTA升级包MD5校验失败
         */
        void onFileMD5CheckFailure();
    }

    /**
     * 线程池的初始化
     * @return 返回线程池
     */
    private static synchronized ExecutorService initExecutorService(){

        if(null == executorService){
            //这里只是给这个线程起一个名字
            ThreadFactory threadFactory = new ThreadFactory() {
                @Override
                public Thread newThread(@NonNull Runnable runnable) {
                    return new Thread(runnable,"OTA Download Thread");
                }
            };
            //这里按照OkHttp的线程池样式来创建，单个线程在闲置的时候保留60秒
            executorService = new ThreadPoolExecutor(0,Integer.MAX_VALUE,60L, TimeUnit.SECONDS,new SynchronousQueue<Runnable>(),threadFactory);
        }
        return executorService;
    }


    /**
     * 开启线程下载固件
     * @param firmwareURL 固件URL
     * @param outputFile  固件要保存的本地全路径及文件名
     * @param md5Sum      用于下载完成后做校验的MD5
     * @param otaDownloadCallback  过程回调
     */
    public static void downloadFirmware(final String firmwareURL, final String outputFile, final String md5Sum,@NonNull final OtaDownloadCallback otaDownloadCallback) {
        initExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                if(downloadThreadRunning){
                    JiotLogger.d("ota download thread is running");
                    return;
                }
                downloadThreadRunning = true;
                int tryTimes = 0;
                do {
                    RandomAccessFile fos = null;
                    InputStream stream = null;
                    try {
                        tryTimes ++;
                        fos = new RandomAccessFile(outputFile, "rw");
                        long downloadBytes = 0;
                        int lastPercent = 0;

                        //建立http连接
                        JiotLogger.d( "connecting : " + firmwareURL);
                        HttpURLConnection conn = createURLConnection(firmwareURL);
                        conn.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
                        conn.setReadTimeout(DEFAULT_READ_TIMEOUT);
                        conn.setRequestProperty("Range", "bytes=" + downloadBytes + "-");
                        conn.connect();

                        int totalLength = conn.getContentLength();
                        JiotLogger.d("totalLength " + totalLength + " bytes");

                        stream = conn.getInputStream();
                        byte[] buffer = new byte[BUFFER_MAX_SIZE];
                        while (downloadBytes < totalLength) {
                            int len = stream.read(buffer);
                            if (len < 0) {
                                break;
                            }
                            downloadBytes += len;
                            fos.write(buffer, 0, len);
                            //计算下载进度
                            int percent = (int) (((float)downloadBytes / (float) totalLength) * 100);
                            if (percent != lastPercent) {
                                lastPercent = percent;
                                //回调到外界显示下载进度
                                otaDownloadCallback.onDownloadProgress(percent);
                                JiotLogger.d("download " + downloadBytes + " bytes. percent:" + percent);
                            }
                        }
                        //下载完成，关闭资源
                        fos.close();
                        if (stream != null) {
                            stream.close();
                        }
                        //回调下载成功
                        otaDownloadCallback.onDownloadSuccess();
                        //计算文件的md5值
                        String calcMD5 = fileToMD5(outputFile);
                        if (!calcMD5.equalsIgnoreCase(md5Sum)) {
                            JiotLogger.e( "md5 checksum not match!!!" + " calculated md5:" + calcMD5);
                            //回调md5校验失败
                            otaDownloadCallback.onFileMD5CheckFailure();
                            //删除文件
                            boolean deleteResule = new File(outputFile).delete();
                            if(!deleteResule) {
                                JiotLogger.d("delete file error.");
                            }
                        } else {
                            //回调md5校验成功
                            otaDownloadCallback.onFileMD5CheckSuccess(outputFile);
                            break;
                        }
                    }catch (CertificateException e) {
                        otaDownloadCallback.onFileMD5CheckFailure();
                    } catch (Exception e) {
                        e.printStackTrace();
                        otaDownloadCallback.onDownloadFailure();
                    }finally {
                        if (fos != null) {
                            try {
                                fos.close();
                            }catch (Exception ignored) {
                            }
                        }
                        if (stream != null) {
                            try {
                                stream.close();
                            }catch (Exception ignored) {
                            }
                        }
                    }
                }while (tryTimes <= MAX_TRY_TIMES);

                downloadThreadRunning = false;
            }
        });
    }


    /**
     * 根据URL创建对应的HTTP或HTTPS连接对象
     * @param firmwareURL 固件URL
     * @return HttpURLConnection或HttpsURLConnection对象
     */
    private static HttpURLConnection createURLConnection(String firmwareURL) throws Exception {

        if (firmwareURL.toLowerCase().startsWith(HTTPS_URL_START)) {
            URL url = new URL(firmwareURL);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            SSLContext sslContext = SSLContext.getInstance("SSL");
            TrustManager[] tm = {new X509TrustManager(){
                @SuppressLint("TrustAllX509TrustManager")
                @Override
                public void checkClientTrusted(X509Certificate[] x509Certificates, String s){
                }
                @SuppressLint("TrustAllX509TrustManager")
                @Override
                public void checkServerTrusted(X509Certificate[] x509Certificates, String s){
                }
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            }};
            sslContext.init(null, tm, new java.security.SecureRandom());
            SSLSocketFactory ssf = sslContext.getSocketFactory();
            conn.setSSLSocketFactory(ssf);
            return conn;
        }
        URL url = new URL(firmwareURL);
        return (HttpURLConnection) url.openConnection();
    }
    /**
     * 计算文件的MD5摘要值
     * @param filePath 全路径文件名
     * @return 以16进制字符表示的摘要字符串
     */
    private static String fileToMD5(String filePath) {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(filePath);
            byte[] buffer = new byte[BUFFER_MAX_SIZE];
            MessageDigest digest = MessageDigest.getInstance("MD5");
            int numRead = 0;
            while (numRead != -1) {
                numRead = inputStream.read(buffer);
                if (numRead > 0) {
                    digest.update(buffer, 0, numRead);
                }
            }
            byte[] md5Bytes = digest.digest();
            return convertHashToString(md5Bytes);
        } catch (Exception e) {
            return "";
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * 转换摘要值为字符串形式
     * @param digestBytes 二进制摘要值
     * @return 以16进制字符表示的摘要字符串
     */
    private static String convertHashToString(byte[] digestBytes) {
        StringBuilder returnVal = new StringBuilder();
        for (byte digestByte : digestBytes) {
            returnVal.append(Integer.toString((digestByte & 0xff) + 0x100, 16).substring(1));
        }
        return returnVal.toString().toLowerCase();
    }

}

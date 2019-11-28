package cn.jiguang.iot.bean;

/**
 * @author : ouyangshengduo
 * e-mail : ouysd@jiguang.cn
 * date  : 2019/7/18 13:32
 * desc : 升级请求中的数据内容
 */
public class OtaUpgradeInform {

    /**任务的id**/
    private long taskId;
    /**升级包文件大小**/
    private int size;
    /**升级版本的下载URL**/
    private String url;
    /**升级包文件的md5值**/
    private String md5;
    /**版本号**/
    private String appVersion;

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    @Override
    public String toString() {
        return "size = " + size + ", url = " + url + ", md5 = " + md5 + ",appVersion = " + appVersion;
    }
}

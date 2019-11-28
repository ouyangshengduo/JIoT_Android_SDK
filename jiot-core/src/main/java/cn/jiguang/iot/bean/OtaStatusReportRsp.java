package cn.jiguang.iot.bean;

/**
 * @author : ouyangshengduo
 * e-mail : ouysd@jiguang.cn
 * date  : 2019/7/18 14:47
 * desc :
 */
public class OtaStatusReportRsp {

    /**回复的序号**/
    private long seqNo;
    /**上报后的错误码**/
    private int code;

    public long getSeqNo() {
        return seqNo;
    }

    public void setSeqNo(long seqNo) {
        this.seqNo = seqNo;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}

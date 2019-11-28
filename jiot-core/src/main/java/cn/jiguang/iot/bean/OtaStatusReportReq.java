package cn.jiguang.iot.bean;

/**
 * @author : ouyangshengduo
 * e-mail : ouysd@jiguang.cn
 * date  : 2019/7/18 14:47
 * desc :
 */
public class OtaStatusReportReq {

    /**上报的消息序号**/
    private long seqNo;
    /**任务id**/
    private long taskId;
    /**当前升级的步骤**/
    private int step;
    /**描述**/
    private String desc;

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    public long getSeqNo() {
        return seqNo;
    }

    public void setSeqNo(long seqNo) {
        this.seqNo = seqNo;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}

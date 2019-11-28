package cn.jiguang.iot.bean;

/**
 * @author : ouyangshengduo
 * e-mail : ouysd@jiguang.cn
 * date  : 2019/7/18 13:31
 * desc : 服务端下发的升级请求信息
 */
public class OtaUpgradeInformReq {

    /**收到的消息序号**/
    private long seqNo;
    /**升级所需要的数据内容**/
    private OtaUpgradeInform data;

    public long getSeqNo() {
        return seqNo;
    }

    public void setSeqNo(long seqNo) {
        this.seqNo = seqNo;
    }

    public OtaUpgradeInform getData() {
        return data;
    }

    public void setData(OtaUpgradeInform data) {
        this.data = data;
    }
}

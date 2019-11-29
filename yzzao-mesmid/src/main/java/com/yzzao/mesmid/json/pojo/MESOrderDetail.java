package com.yzzao.mesmid.json.pojo;

public class MESOrderDetail {

    private String billno;
    private String notice;
    private Integer orderschedule;
    private String machineid;
    public MESOrderDetail(){}
    public MESOrderDetail( String BillNo,
     String Notice,
     Integer OrderSchedule,
     String MachineID){
        this.billno = BillNo;
        this.notice=Notice;
        this.orderschedule=OrderSchedule;
        this.machineid=MachineID;
    }
    public String getBillno() {
        return billno;
    }
    public void setBillno(String billno) {
        this.billno = billno;
    }
    public String getNotice() {
        return notice;
    }
    public void setNotice(String notice) {
        this.notice = notice;
    }
    public Integer getOrderschedule() {
        return orderschedule;
    }
    public void setOrderschedule(Integer orderschedule) {
        this.orderschedule = orderschedule;
    }
    public String getMachineid() {
        return machineid;
    }
    public void setMachineid(String machineid) {
        this.machineid = machineid;
    }

}
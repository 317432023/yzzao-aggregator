package com.yzzao.client.spec.struct;
/**
 * 指令头
 * @author kangtengjiao
 *
 */
public class Header {
	/**通讯序号2bytes*/
	private String comSeqNo;//readUnsignedShort(); String.format("%04x",comSeqNo) 或者 readBytes(byte[] dst,  2);bytesToHex(dst)
	/**时间戳4bytes*/
	private String ts;//readUnsignedInt(); String.format("%08x",ts) 或者 readBytes(byte[] dst,  4);bytesToHex(dst)
	/**请求方IDc: 厂商信息代码+设备类型代码+设备序列码 4Byte+3Byte+9Byte*/
	private Guid reqGuid;
	/**应答方IDc: 厂商信息代码+设备类型代码+设备序列码 4Byte+3Byte+9Byte*/
	private Guid respGuid;
	/**信息内容总长度4bytes*/
	private int length;//readInt();
	
	public String getComSeqNo() {
		return comSeqNo;
	}
	public void setComSeqNo(String comSeqNo) {
		this.comSeqNo = comSeqNo;
	}
	public String getTs() {
		return ts;
	}
	public void setTs(String ts) {
		this.ts = ts;
	}
	public Guid getReqGuid() {
		return reqGuid;
	}
	public void setReqGuid(Guid reqGuid) {
		this.reqGuid = reqGuid;
	}
	public Guid getRespGuid() {
		return respGuid;
	}
	public void setRespGuid(Guid respGuid) {
		this.respGuid = respGuid;
	}
	public int getLength() {
		return length;
	}
	public void setLength(int length) {
		this.length = length;
	}
}

package com.yzzao.client.spec.struct;

public class Guid {

	/**厂商信息代码4bytes+设备类型代码3bytes+设备序列码9bytes*/
	private String merNo;//readUnsignedInt(); String.format("%08x",merNo); 或者 readBytes(byte[] dst,  4);bytesToHex(dst)
	private String devTyNo;//readUnsignedMedium(); String.format("%06x",devTyNo); 或者readBytes(byte[] dst,  3);bytesToHex(dst)
	private String devSeqNo;//readBytes(byte[] dst,  9); bytesToHex(dst)
	
	public String getMerNo() {
		return merNo;
	}
	public void setMerNo(String merNo) {
		this.merNo = merNo;
	}
	public String getDevTyNo() {
		return devTyNo;
	}
	public void setDevTyNo(String devTyNo) {
		this.devTyNo = devTyNo;
	}
	public String getDevSeqNo() {
		return devSeqNo;
	}
	public void setDevSeqNo(String devSeqNo) {
		this.devSeqNo = devSeqNo;
	}
}

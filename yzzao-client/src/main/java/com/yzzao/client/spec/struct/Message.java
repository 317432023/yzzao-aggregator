package com.yzzao.client.spec.struct;

import com.yzzao.common.utils.StringUtil;

public class Message {
	/**指令头*/
	private Header header;
	
	// 以下为body 内容
	
	/**功能类型代码1byte*/
	private byte funTpNo;//readByte()
	/**子功能类型代码1byte*/
	private byte subFunTpNo;//readByte()
	
	/**参数（代码）可变长度n*/
	private String parCd;//readBytes(byte[]);new String(byte[],"UTF-8")
	
	public Header getHeader() {
		return header;
	}
	public void setHeader(Header header) {
		this.header = header;
	}
	public byte getFunTpNo() {
		return funTpNo;
	}
	public void setFunTpNo(byte funTpNo) {
		this.funTpNo = funTpNo;
	}
	public byte getSubFunTpNo() {
		return subFunTpNo;
	}
	public void setSubFunTpNo(byte subFunTpNo) {
		this.subFunTpNo = subFunTpNo;
	}
	public String getParCd() {
		return parCd;
	}
	public void setParCd(String parCd) {
		this.parCd = parCd;
	}
	
    /**
     * 生成完整消息对应的字节数组，如果没有消息体，就只有头部
     * 20181207 System.arrayCopy 是线程不安全的
     * 20181208 改成静态方法
     * @return
     */
    public synchronized static byte[] composeFull(Message msg) {
        
        if (msg == null || msg.getHeader() == null)
            return null;
        
        byte[] array = new byte[ 2+4+(4+3+9)*2 + 4 + 2 + (msg.getParCd()!=null?msg.getParCd().length()/2:0)];
        
        // 指令头
        Header header = msg.getHeader();
        
        // 通讯序号
        String comSeqNo = header.getComSeqNo();
        //out.writeBytes(StringUtil.hexToBytes(comSeqNo));
        System.arraycopy(StringUtil.hexToBytes(comSeqNo), 0, array, 0, 2);
        
        // 时间戳
        String ts = header.getTs();
        //out.writeBytes(StringUtil.hexToBytes(ts));
        System.arraycopy(StringUtil.hexToBytes(ts), 0, array, 2, 4);

        // 请求方IDc
        Guid reqGuid = header.getReqGuid();
        //writeGuid(out, reqGuid);
        System.arraycopy(StringUtil.hexToBytes(reqGuid.getMerNo()), 0, array, 2+4, 4);
        System.arraycopy(StringUtil.hexToBytes(reqGuid.getDevTyNo()), 0, array, 6+4, 3);
        System.arraycopy(StringUtil.hexToBytes(reqGuid.getDevSeqNo()), 0, array, 10+3, 9);
        
        // 应答方IDc
        Guid respGuid  = header.getRespGuid();
        //writeGuid(out, respGuid);
        System.arraycopy(StringUtil.hexToBytes(respGuid.getMerNo()), 0, array, 13+9, 4);
        System.arraycopy(StringUtil.hexToBytes(respGuid.getDevTyNo()), 0, array, 22+4, 3);
        System.arraycopy(StringUtil.hexToBytes(respGuid.getDevSeqNo()), 0, array, 26+3, 9);
        
        // 信息内容总长度(在计算完后面的参数代码长度后确定)
//      int length = header.getLength();
        int length = msg.getParCd() != null ? 2+msg.getParCd().length()/2 : 2;
        //out.writeInt(length);
        System.arraycopy(StringUtil.intToBytes2(length), 0, array, 29+9, 4);
        
        
        // 功能类型代码
        //out.writeByte(msg.getFunTpNo());
        System.arraycopy(new byte[] {msg.getFunTpNo()}, 0, array, 38+4, 1);
        
        // 子功能类型代码
        //out.writeByte(msg.getSubFunTpNo());
        System.arraycopy(new byte[] {msg.getSubFunTpNo()}, 0, array, 42+1, 1);
        
        // 参数（代码）可变长度n
        if(msg.getParCd() != null) {
            //out.writeBytes(StringUtil.hexToBytes(msg.getParCd()));
            byte[] tb = StringUtil.hexToBytes(msg.getParCd());
            System.arraycopy(tb, 0, array, 43+1, length - 2);
        }
        //System.out.println(StringUtil.bytesToHex(array));
        return array;
    }
    @Override
    public String toString() {
        return StringUtil.bytesToHex(composeFull(this));
    }
    
    // 20181122添加
    private boolean alarm = false;
    
    public boolean isAlarm() {
        return alarm;
    }
    public void setAlarm(boolean alarm) {
        this.alarm = alarm;
    }
}

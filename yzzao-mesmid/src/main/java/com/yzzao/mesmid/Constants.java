package com.yzzao.mesmid;

public class Constants {

    public static int UDPPort;
    public static String REQ_URL_V1,REQ_URL_V2;
    public static String APPID;
    public static String APPSecret;
    public static boolean IS_SEND;
    
    public static String WORK_MODE;
    
    public static String ServIP;
    public static int TCPServPort;
    
    public static int HttpServPort;
    
    public static String WS_ADDR,WS_START_FLAG,WS_START_URL;
    /** ws心跳检测与重连时间间隔，单位毫秒*/
    public static int WS_HEARTBEAT_INTERVAL,WS_RECONNECT_INTERVAL;
    
    /** 下位机总数*/
    public static int totalMachine;
    /** 下位机每隔多久产生一个数据包（单位秒）*/
    public static int packageFrequency;
    /** 单线程转发平均耗时(单位毫秒) */
    public static int avgPostCost;

    /** 转发消费者线程数*/
    public static int transThreadsCountV1,transThreadsCountV2;

    /** 转发线程睡眠时间，单位毫秒*/
    public static int transmitSleepTime;
    
    /** MES 系统 判断断网的阈值或临界值（单位毫秒） */
    public static int mesOffLineThreshold;
    
    /** 当MES某台机器达到掉线临界点时中间层收发策略 : 0 - 继续收发不变, 1 - 忽略转发队列头的对应该机器的数据包直到掉线恢复 */
    public static int transmitStrategy;
    
    /** post 建立连接超时设置（包含建立连接 和 读取 返回结果 的时间 ， 单位毫秒）*/
    public static int connectTimeout;
    /** post 读取 返回结果超时设置（包含建立连接 和 读取 返回结果 的时间 ， 单位毫秒）*/
    public static int readTimeout;
    
    /** 日志里是否记录成功转发的数据包 0 - 不记录*/
    public static int logSuccessPack =0;
    
    /** 间隔多久发送转数给手持机，单位秒。能否发送成功取决于MES和手机与中间层连接是否稳定*/
    public static int sendTurnCountInterval = 600;
    
    /** 扫描文件路径*/
    public static String scanFilePath = null;
    
}

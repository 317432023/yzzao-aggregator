package com.yzzao.client.multicli;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.yzzao.client.spec.client.OriknitClient;
import com.yzzao.common.utils.DateUtil;
import com.yzzao.common.utils.StringUtil;

public class TimerTask implements Runnable {
    private final static Logger logger = Logger.getLogger(TimerTask.class);
    final int timeoutSec = 6;
    private volatile Map<Integer, Long> timer;
    private volatile Map<Integer, OriknitClient> specClientMap;
    public TimerTask(Map<Integer, Long> timer,Map<Integer, OriknitClient> specClientMap){
        this.timer = timer;
        this.specClientMap = specClientMap;}
    @Override
    public void run() {
        for(;;) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Set<Integer> gaugeSet = timer.keySet();
            for(Integer gauge : gaugeSet) {
                Long lastRecv = timer.get(gauge);
                
                //Date d =new Date();d.setTime(lastRecv.longValue());
                //logger.info(StringUtil.bytesToHex(StringUtil.intToBytes2(gauge))+"=>"+(new SimpleDateFormat(DateUtil.CHN_TIM_FORMAT)).format(d));
                
                if(System.currentTimeMillis() - lastRecv > timeoutSec*1000) {
                    logger.info("下位机超时移除服务台连接["+StringUtil.bytesToHex(StringUtil.intToBytes2(gauge))+"]");
                    timer.remove(gauge);
                    OriknitClient cli = specClientMap.get(gauge);
                    specClientMap.remove(gauge);
                    if(cli!=null) {
                        cli.getFuture().channel().close();
                    }
                }
            }
        }
    }

}

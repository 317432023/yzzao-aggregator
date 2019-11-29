 package com.yzzao.common.utils;
 
 import java.io.PrintStream;
 import java.text.ParseException;
 import java.text.SimpleDateFormat;
 import java.util.Calendar;
 import java.util.Date;
 import org.apache.log4j.Logger;
 
 public final class DateUtil
 {
   private static final Logger log = Logger.getLogger(DateUtil.class);
   
   public static final String CHN_FULL_FORMAT = "yyyy-MM-dd HH:mm:ss SSS";
   
   public static final String CHN_LONG_FORMAT = "yyyy-MM-dd HH:mm:ss";
   
   public static final String CHN_MIDD_FORMAT = "yyyy-MM-dd HH:mm";
   
   public static final String CHN_SHRT_FORMAT = "yyyy-MM-dd";
   
   public static final String CHN_TIM_FORMAT = "HH:mm:ss";
   
 
   public static String getDate(Date utd, String format)
   {
     SimpleDateFormat sdf = new SimpleDateFormat(format);
     
     String s = sdf.format(utd);
     
     return s;
   }
   
 
 
 
 
   public static Date getDate(String ds, String format)
   {
     SimpleDateFormat sdf = new SimpleDateFormat(format);
     
     Date d = null;
     try {
       d = sdf.parse(ds);
     } catch (ParseException e) {
       log.error("字符串转日期失败", e);
     }
     
     return d;
   }
   
 
 
 
 
   public static Date getDayDate(Date date)
   {
     Calendar cal = Calendar.getInstance();
     cal.setTime(date);
     cal.set(11, 0);
     cal.set(12, 0);
     cal.set(13, 0);
     cal.set(14, 0);
     return cal.getTime();
   }
   
 
 
 
 
 
 
 
   public static int compare(Date d1, Date d2)
   {
     long dt1 = d1.getTime();
     long dt2 = d2.getTime();
     
     if (dt1 > dt2) return 1;
     if (dt1 < dt2) { return -1;
     }
     return 0;
   }
   
 
 
 
 
 
 
   public static Date addDay(Date curDate, int days)
   {
     Calendar cal = Calendar.getInstance();
     cal.setTime(curDate);
     
     cal.add(5, days);
     
     return cal.getTime();
   }
   
 
 
 
 
 
   public static Date addSecond(Date curDate, int seconds)
   {
     Calendar cal = Calendar.getInstance();
     cal.setTime(curDate);
     
     cal.add(13, seconds);
     
     return cal.getTime();
   }
   
   private static int diff(Date d1, Date d2, int scale)
   {
     long l1 = d1.getTime();
     long l2 = d2.getTime();
     int diff = (int)(l1 - l2);
     switch (scale) {
     case 0:  return diff;
     case 1:  return diff / 1000;
     case 2:  return diff / 60000;
     case 3:  return diff / 3600000;
     case 4:  return diff / 86400000;
     }
     return diff;
   }
   
   public static int diffSecond(Date d1, Date d2) {
     return diff(d1, d2, 1);
   }
   
   public static void main(String[] args) {
     Date newDate = addDay(getDate("20170531", "yyyyMMdd"), 1);
     
     System.out.println(newDate);
     
     System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS").format(getDayDate(new Date())));
     
     System.out.println(diffSecond(getDate("20170531", "yyyyMMdd"), getDate("20170530", "yyyyMMdd")));
   }
 }

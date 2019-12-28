#!/bin/bash
mesdir="/usr/local/mesmid"
cd $mesdir
#echo $mesdir/lib
export CLASSPATH=`find $mesdir/lib -name  *.jar | xargs | sed  "s/ /:/g"`
#echo $CLASSPATH
cd $mesdir/lib
unset mesdir
nohup java -Dfile.encoding=UTF-8 -Dcom.sun.management.jmxremote.port=9898 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dlog4j.configuration="file:/usr/local/mesmid/conf/log4j.properties" -Dio.netty.leakDetectionLevel=ADVANCED -Xms512m -Xmx512m -cp $CLASSPATH com.yzzao.mesmid.MainApp /usr/local/mesmid/conf/app.properties > /dev/null 2>&1 &


package com.yzzao.common.utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;


/**
 * java如何得到GET和POST请求URL和参数列表
一 获取URL:
getRequestURL()

二 获取参数列表:

1.getQueryString()

只适用于GET,比如客户端发送http://localhost/testServlet?a=b&c=d&e=f,通过request.getQueryString()得到的是a=b&c=d&e=f.


2.getParameter()
GET和POST都可以使用
但如果是POST请求要根据<form>表单提交数据的编码方式来确定能否使用.
当编码方式是(application/x-www-form-urlencoded)时才能使用.这种编码方式虽然简单，但对于传输大块的二进制数据显得力不从心.
对于传输大块的二进制数这类数据，浏览器采用了另一种编码方式("multipart/form-data"),这时就需要使用下面的两种方法.

3.getInputStream()
4.getReader()
上面两种方法获取的是Http请求包的包体,因为GET方式请求一般不包含包体.所以上面两种方法一般用于POST请求获取参数.

需要注意的是：
request.getParameter()、 request.getInputStream()、request.getReader()这三种方法是有冲突的，因为流只能被读一次。
比如：
当form表单内容采用 enctype=application/x-www-form-urlencoded编码时，先通过调用request.getParameter()方法得到参数后,
再调用request.getInputStream()或request.getReader()已经得不到流中的内容，
因为在调用 request.getParameter()时系统可能对表单中提交的数据以流的形式读了一次,反之亦然。

当form表单内容采用 enctype=multipart/form-data编码时，即使先调用request.getParameter()也得不到数据，
所以这时调用request.getParameter()方法对 request.getInputStream()或request.getReader()没有冲突，
即使已经调用了 request.getParameter()方法也可以通过调用request.getInputStream()或request.getReader()得到表单中的数据,
而request.getInputStream()和request.getReader()在同一个响应中是不能混合使用的,如果混合使用就会抛异常。
 * 
 */
public class HttpUtil {
	private final static Logger logger = Logger.getLogger(HttpUtil.class);
	
	/** post 默认超时设置（包含建立连接 和 读取 返回结果 的时间 ， 单位毫秒）*/
	private final static int DEFAULT_connectTimeout = 2000;
	private final static int DEFAULT_readTimeout = 2000;
	
	/**
	 * 
	 * 类名: MyX509TrustManager </br> 描述: 信任管理器 </br>
	 * 这个证书管理器的作用就是让它信任我们指定的证书，上面的代码意味着信任所有证书，不管是否权威机构颁发
	 */
	public static class MyX509TrustManager implements X509TrustManager {

		// 检查客户端证书
		public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		}

		// 检查服务器端证书
		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		}

		// 返回受信任的X509证书数组
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}
	}
	
	public static String post(String reqUrl, Map<String, String> map) {
		return post( reqUrl,  map, DEFAULT_connectTimeout, DEFAULT_readTimeout, false);
	}
	
	/**
	 * POST原始数据
	 * 
	 * @param reqUrl
	 * @param jsonStr
	 */
	public static String post(String reqUrl, Map<String, String> map, int connectTimeout, int readTimeout, boolean useSSL) {

		StringBuffer sbr = new StringBuffer(map.size()*20);
		for (String k : map.keySet()) {
			sbr.append(k).append('=').append(map.get(k)).append('&');
		}
		if (sbr.length() > 0) {
			sbr.deleteCharAt(sbr.length() - 1);
		}

		if (logger.isDebugEnabled()) {
			logger.debug(sbr.toString());
		}
		
		DataOutputStream dosOut = null;
		PrintWriter pwOut = null;
		OutputStreamWriter osw = null;
		BufferedReader in = null;
		HttpURLConnection connection = null;
		try {
			// 创建连接
			URL url = null;

			if(useSSL) {
				url= new URL(null, reqUrl, new sun.net.www.protocol.https.Handler());
				connection = (HttpsURLConnection) url.openConnection();
				// 创建SSLContext对象，并使用我们指定的信任管理器初始化
				TrustManager[] tm = { new MyX509TrustManager() };
				SSLContext sslContext = SSLContext.getInstance("SSL", "SunJSSE");
				sslContext.init(null, tm, new java.security.SecureRandom());
				// 从上述SSLContext对象中得到SSLSocketFactory对象
				SSLSocketFactory ssf = sslContext.getSocketFactory();
				((HttpsURLConnection)connection).setSSLSocketFactory(ssf);
			}else{
				url= new URL(reqUrl);
				connection = (HttpURLConnection)url.openConnection();
			}
			
			connection.setDoOutput(true);
			connection.setDoInput(true);
			connection.setRequestMethod("POST");
			connection.setUseCaches(false);
			connection.setInstanceFollowRedirects(true);
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			connection.setRequestProperty("Charset", "utf-8");
			connection.setRequestProperty("Connection", "Keep-Alive");// 设置长连接
			connection.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
			connection.setConnectTimeout(connectTimeout);// 设置连接超时连接超时，单位ms
			connection.setReadTimeout(readTimeout);// 设置响应读取超时，单位ms
			connection.connect();

			// POST方式一：DataOutputStream写字节数组
			// dosOut = new DataOutputStream(connection.getOutputStream());
			// dosOut.writeBytes(jsonStr);
			// dosOut.flush();

			// POST方式二：PrintWriter写参数 name1=value1&name2=value2 的形式
			pwOut = new PrintWriter(connection.getOutputStream());
			pwOut.print(sbr.toString());// 发送请求参数
			pwOut.flush();// flush输出流的缓冲

			// POST方式三： OutputStreamWriter写参数 name1=value1&name2=value2 的形式
			// osw = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
			// osw.write(sbr.toString());
			// osw.flush();

			// 读取响应
			in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String lines;
			StringBuffer sb = new StringBuffer("");
			while ((lines = in.readLine()) != null) {
				lines = new String(lines.getBytes(), "utf-8");
				sb.append(lines);
			}

			return sb.toString();
		} catch (MalformedURLException e) {
			logger.error(e.getMessage());
		} catch (UnsupportedEncodingException e) {
			logger.error(e.getMessage());
		} catch (IOException e) {
			logger.error(e.getMessage());
		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			try {
				if (dosOut != null)
					dosOut.close();
				if (in != null)
					in.close();
				if (osw != null)
					osw.close();
			} catch (IOException e) {
				logger.error(e.toString());
			}

			if (pwOut != null) {
				pwOut.close();
			}
			
			// 断开连接
			if(connection!=null) {
				connection.disconnect();
			}
		}
		return null;
	}

	
	/**
     * 向指定URL发送GET方法的请求
     *
     * @param url
     *            发送请求的URL
     * @param param
     *            请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @param connectTimeout  
     * 			     连接超时毫秒
     * @param readTimeout
     * 			     读取超时毫秒
     * @return URL 所代表远程资源的响应结果
     */
    public static String get(String url, String param, int connectTimeout, int readTimeout) {
        String result = "";
        BufferedReader in = null;
        try {
            String urlNameString = url+ (StringUtils.isNotBlank(param)?("?" + param):"");
            URL realUrl = new URL(urlNameString);
            // 打开和URL之间的连接
            HttpURLConnection connection = (HttpURLConnection)realUrl.openConnection();
            // 设置通用的请求属性
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            connection.setConnectTimeout(connectTimeout);// 设置连接超时连接超时，单位ms
			connection.setReadTimeout(readTimeout);// 设置响应读取超时，单位ms
            // 建立实际的连接
            connection.connect();
            
            // 获取所有响应头字段
            Map<String, /*List<String>*/?> map = connection.getHeaderFields();
            // 遍历所有的响应头字段
            for (String key : map.keySet()) {
                //System.out.println(key + "--->" + map.get(key));
            	if(logger.isInfoEnabled()) {
            		logger.info(key + "--->" + map.get(key));
            	}
            }
            
            // 定义 BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
            
        } catch (Exception e) {
            //System.err.println("发送GET请求出现异常！" + e);
            logger.error(e.getMessage());
        } finally {
        	// 使用finally块来关闭输入流
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e2) {
                //e2.printStackTrace();
                logger.error(e2.getMessage());
            }
        }
        return result;
    }

    public static void main(String[] args){
    	final String url = "http://mesapi.yzzao.com";
    	final String param = null;
    	final String result = HttpUtil.get(url, param, DEFAULT_connectTimeout, DEFAULT_readTimeout);
    	System.out.println(result);
    }
	
}
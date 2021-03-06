package org.liufeng.weixin.util;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.liufeng.course.message.resp.Button;
import org.liufeng.course.message.resp.ClickButton;
import org.liufeng.course.message.resp.Menu;
import org.liufeng.course.message.resp.ViewButton;
import org.liufeng.weixin.pojo.AccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 公共平台通用接口工具类
 * cn.suunyboy.wechat.resp
 * author:HUGUANG
 * version:v1.0
 * time:2016-11-24 下午4:44:16
 * email:940728678@qq.com
 */
public class WeixinUtil {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(WeixinUtil.class);

	// 菜单创建（POST） 限100（次/天）   
	public static String menu_create_url = "https://api.weixin.qq.com/cgi-bin/menu/create?access_token=ACCESS_TOKEN"; 
	
	// 获取access_token的接口地址（GET） 限200（次/天）   
	public final static String access_token_url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=APPID&secret=APPSECRET";  
	
	public final static String UPLOAD_URL = "https://api.weixin.qq.com/cgi-bin/media/upload?access_token=ACCESS_TOKEN&type=TYPE";

	//调用创建接口创建菜单
	public final static String CREATE_MENU = "https://api.weixin.qq.com/cgi-bin/menu/create?access_token=ACCESS_TOKEN";
	
	//调用查询接口菜单
	public final static String QUERY_MENU = "https://api.weixin.qq.com/cgi-bin/menu/get?access_token=ACCESS_TOKEN";

	//调用删除接口菜单
	public final static String DELETE_MENU = "https://api.weixin.qq.com/cgi-bin/menu/delete?access_token=ACCESS_TOKEN";
	/** 
	 * 获取access_token 
	 *  
	 * @param appid 凭证 
	 * @param appsecret 密钥 
	 * @return 
	 */  
	public static AccessToken getAccessToken(String appid, String appsecret) {  
	    AccessToken accessToken = null;  
	  
	    String requestUrl = access_token_url.replace("APPID", appid).replace("APPSECRET", appsecret);  
	    JSONObject jsonObject = httpRequest(requestUrl, "GET", null);  
	    // 如果请求成功   
	    if (null != jsonObject) {  
	        try {  
	            accessToken = new AccessToken();  
	            accessToken.setToken(jsonObject.getString("access_token"));  
	            accessToken.setExpiresIn(jsonObject.getInt("expires_in"));  
	        } catch (JSONException e) {  
	            accessToken = null;  
	            // 获取token失败   
//	            log.error("获取token失败 errcode:{} errmsg:{}", jsonObject.getInt("errcode"), jsonObject.getString("errmsg"));  
	        }  
	    }  
	    return accessToken;  
	} 

	
	/**
	 * 发起https请求并获取结果
	 * 
	 * @param requestUrl 请求地址
	 * @param requestMethod 请求方式（GET、POST）
	 * @param outputStr 提交的数据
	 * @return JSONObject(通过JSONObject.get(key)的方式获取json对象的属性值)
	 */
	public static JSONObject httpRequest(String requestUrl, String requestMethod, String outputStr) {
		JSONObject jsonObject = null;
		StringBuffer buffer = new StringBuffer();
		try {
			// 创建SSLContext对象，并使用我们指定的信任管理器初始化
			TrustManager[] tm = { new MyX509TrustManager() };
			SSLContext sslContext = SSLContext.getInstance("SSL", "SunJSSE");
			sslContext.init(null, tm, new java.security.SecureRandom());
			// 从上述SSLContext对象中得到SSLSocketFactory对象
			SSLSocketFactory ssf = sslContext.getSocketFactory();

			URL url = new URL(requestUrl);
			HttpsURLConnection httpUrlConn = (HttpsURLConnection) url.openConnection();
			httpUrlConn.setSSLSocketFactory(ssf);

			httpUrlConn.setDoOutput(true);
			httpUrlConn.setDoInput(true);
			httpUrlConn.setUseCaches(false);
			// 设置请求方式（GET/POST）
			httpUrlConn.setRequestMethod(requestMethod);

			if ("GET".equalsIgnoreCase(requestMethod))
				httpUrlConn.connect();

			// 当有数据需要提交时
			if (null != outputStr) {
				OutputStream outputStream = httpUrlConn.getOutputStream();
				// 注意编码格式，防止中文乱码
				outputStream.write(outputStr.getBytes("UTF-8"));
				outputStream.close();
			}

			// 将返回的输入流转换成字符串
			InputStream inputStream = httpUrlConn.getInputStream();
			InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "utf-8");
			BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

			String str = null;
			while ((str = bufferedReader.readLine()) != null) {
				buffer.append(str);
			}
			bufferedReader.close();
			inputStreamReader.close();
			// 释放资源
			inputStream.close();
			inputStream = null;
			httpUrlConn.disconnect();
			jsonObject = JSONObject.fromObject(buffer.toString());
		} catch (ConnectException ce) {
	//		log.error("Weixin server connect timed out.");
		} catch (Exception e) {
	//		log.error("https request error:{}", e);
		}
		return jsonObject;
	}

	public static String upload(String path, String accessToken, String type) throws IOException{
		File file = new File(path);
		if (!file.exists() || !file.isFile()) {
			throw new IOException("文件不存在");
		}

		String url = UPLOAD_URL.replace("ACCESS_TOKEN", accessToken).replace("TYPE",type);
		
		URL urlObj = new URL(url);
		//连接
		HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();

		con.setRequestMethod("POST"); 
		con.setDoInput(true);
		con.setDoOutput(true);
		con.setUseCaches(false); 

		//设置请求头信息
		con.setRequestProperty("Connection", "Keep-Alive");
		con.setRequestProperty("Charset", "UTF-8");

		//设置边界
		String BOUNDARY = "----------" + System.currentTimeMillis();
		con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);

		StringBuilder sb = new StringBuilder();
		sb.append("--");
		sb.append(BOUNDARY);
		sb.append("\r\n");
		sb.append("Content-Disposition: form-data;name=\"file\";filename=\"" + file.getName() + "\"\r\n");
		sb.append("Content-Type:application/octet-stream\r\n\r\n");

		byte[] head = sb.toString().getBytes("utf-8");

		//获得输出流
		OutputStream out = new DataOutputStream(con.getOutputStream());
		//输出表头
		out.write(head);

		//文件正文部分
		//把文件已流文件的方式 推入到url中
		DataInputStream in = new DataInputStream(new FileInputStream(file));
		int bytes = 0;
		byte[] bufferOut = new byte[1024];
		while ((bytes = in.read(bufferOut)) != -1) {
			out.write(bufferOut, 0, bytes);
		}
		in.close();

		//结尾部分
		byte[] foot = ("\r\n--" + BOUNDARY + "--\r\n").getBytes("utf-8");//定义最后数据分隔线

		out.write(foot);

		out.flush();
		out.close();

		StringBuffer buffer = new StringBuffer();
		BufferedReader reader = null;
		String result = null;
		try {
			//定义BufferedReader输入流来读取URL的响应
			reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String line = null;
			while ((line = reader.readLine()) != null) {
				buffer.append(line);
			}
			if (result == null) {
				result = buffer.toString();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				reader.close();
			}
		}

		JSONObject jsonObj = JSONObject.fromObject(result);
		System.out.println(jsonObj);
		String typeName = "media_id";
		//if(!"image".equals(type)){
		//	typeName = type + "_media_id";
		//}
		String mediaId = jsonObj.getString(typeName);
		return mediaId;
	}
	
	/**
	 * 组装菜单
	 * @return Menu
	 */
	public static Menu initMenu(){
		//组装菜单
		Menu menu = new Menu();
		
		ClickButton clickButton11 = new ClickButton();
		clickButton11.setName("github库");
		clickButton11.setType("click");
		clickButton11.setKey("clickButon1");
		
		ViewButton viewButton21 = new ViewButton();
		viewButton21.setName("view菜单");
		viewButton21.setType("view");
		viewButton21.setUrl("https://github.com/uhgagnu");
		
		ClickButton clickButton31 = new ClickButton();
		clickButton31.setName("扫码事件");
		clickButton31.setType("pic_weixin");
		clickButton31.setKey("31");
		
		ClickButton clickButton32 = new ClickButton();
		clickButton32.setName("地理位置");
		clickButton32.setType("location_select");
		clickButton32.setKey("32");
		
		Button button = new Button();
		button.setName("sub菜单");
		button.setSub_button(new Button[]{clickButton31, clickButton32});
		
		menu.setButton(new Button[]{clickButton11,viewButton21,button});
		//返回Menu对象
		return menu;
	}
	
	/**
	 * 创建菜单
	 * @param token
	 * @param requestMenu
	 * @return
	 */
	public static int createMenu(String token, String requestMenu){
		int resultErrcode = 0;
		String url = CREATE_MENU.replace("ACCESS_TOKEN", token);
		JSONObject resultjson = httpRequest(url, "POST", requestMenu);
		if (resultjson!=null) {
			resultErrcode = resultjson.getInt("errcode");
		}
		return resultErrcode;
	}
	
	/**
	 * 查询菜单
	 * @param token
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static JSONObject queryMenu(String token) throws ClientProtocolException, IOException{
		String url =QUERY_MENU.replace("ACCESS_TOKEN", token);
		JSONObject jsonObject = doGetRequest(url);
		return jsonObject;
	}
	
	/**
	 * 删除菜单
	 * @param token
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static int deleteMenu(String token) throws ClientProtocolException, IOException{
		String url = DELETE_MENU.replace("ACCESS_TOKEN", token);
		JSONObject jsonObject = doGetRequest(url);
		int resultCode = 0;
		if (jsonObject!=null) {
			resultCode = jsonObject.getInt("errcode");
		}
		return resultCode;
	}
	
	/**
	 * 组装get请求
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	public static JSONObject doGetRequest(String url) throws ClientProtocolException, IOException{
		DefaultHttpClient client = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(url);
		JSONObject jsonObject = null;
		HttpResponse httpResponse = client.execute(httpGet);
		HttpEntity entity = httpResponse.getEntity();
		if (null != entity) {
			String result = EntityUtils.toString(entity);
			jsonObject = JSONObject.fromObject(result);
		}
		return jsonObject;
	}
	
	/**
	 * 组装POST请求
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	public static JSONObject doPostRequest(String url, String outStr) throws ClientProtocolException, IOException{
		DefaultHttpClient client = new DefaultHttpClient();
		HttpPost httpPost = new HttpPost(url);
		JSONObject jsonObject = null;
		httpPost.setEntity(new StringEntity(outStr, "UTF-8"));
		HttpResponse httpResponse = client.execute(httpPost);
		String result = EntityUtils.toString(httpResponse.getEntity());
		jsonObject = JSONObject.fromObject(result);
		return jsonObject;
	}
}
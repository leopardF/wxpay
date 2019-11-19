package com.pay.wxpay.utils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.pay.wxpay.utils.WXPayConstants.SignType;

public class WXPayUtil {

	private static final String SYMBOLS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

	private static final Random RANDOM = new SecureRandom();

	/**
	 * XML格式字符串转换为Map
	 *
	 * @param strXML
	 *            XML字符串
	 * @return XML数据转换后的Map
	 * @throws Exception
	 */
	public static Map<String, String> xmlToMap(String strXML) throws Exception {
		try {
			Map<String, String> data = new HashMap<String, String>();
			DocumentBuilder documentBuilder = WXPayXmlUtil.newDocumentBuilder();
			InputStream stream = new ByteArrayInputStream(strXML.getBytes("UTF-8"));
			org.w3c.dom.Document doc = documentBuilder.parse(stream);
			doc.getDocumentElement().normalize();
			NodeList nodeList = doc.getDocumentElement().getChildNodes();
			for (int idx = 0; idx < nodeList.getLength(); ++idx) {
				Node node = nodeList.item(idx);
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					org.w3c.dom.Element element = (org.w3c.dom.Element) node;
					data.put(element.getNodeName(), element.getTextContent());
				}
			}
			try {
				stream.close();
			} catch (Exception ex) {
				// do nothing
			}
			return data;
		} catch (Exception ex) {
			WXPayUtil.getLogger().warn("Invalid XML, can not convert to map. Error message: {}. XML content: {}",
					ex.getMessage(), strXML);
			throw ex;
		}

	}

	/**
	 * 将Map转换为XML格式的字符串
	 *
	 * @param data
	 *            Map类型数据
	 * @return XML格式的字符串
	 * @throws Exception
	 */
	public static String mapToXml(Map<String, String> data) throws Exception {
		org.w3c.dom.Document document = WXPayXmlUtil.newDocument();
		org.w3c.dom.Element root = document.createElement("xml");
		document.appendChild(root);
		for (String key : data.keySet()) {
			String value = data.get(key);
			if (value == null) {
				value = "";
			}
			value = value.trim();
			org.w3c.dom.Element filed = document.createElement(key);
			filed.appendChild(document.createTextNode(value));
			root.appendChild(filed);
		}
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		DOMSource source = new DOMSource(document);
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		StringWriter writer = new StringWriter();
		StreamResult result = new StreamResult(writer);
		transformer.transform(source, result);
		String output = writer.getBuffer().toString(); // .replaceAll("\n|\r",
														// "");
		try {
			writer.close();
		} catch (Exception ex) {
		}
		return output;
	}

	/**
	 * 生成带有 sign 的 XML 格式字符串（MD5）
	 *
	 * @param data
	 *            Map类型数据
	 * @param key
	 *            API密钥
	 * @return 含有sign字段的XML
	 */
	public static String generateSignedXml(final Map<String, String> data, String key) throws Exception {
		return generateSignedXml(data, key, SignType.MD5);
	}

	/**
	 * 生成带有 sign 的 XML 格式字符串
	 *
	 * @param data
	 *            Map类型数据
	 * @param key
	 *            API密钥
	 * @param signType
	 *            签名类型
	 * @return 含有sign字段的XML
	 */
	public static String generateSignedXml(final Map<String, String> data, String key, SignType signType)
			throws Exception {
		String sign = generateSignature(data, key, signType);
		data.put(WXPayConstants.FIELD_SIGN, sign);
		return mapToXml(data);
	}

	/**
	 * 判断签名是否正确
	 *
	 * @param xmlStr
	 *            XML格式数据
	 * @param key
	 *            API密钥
	 * @return 签名是否正确
	 * @throws Exception
	 */
	public static boolean isSignatureValid(String xmlStr, String key) throws Exception {
		Map<String, String> data = xmlToMap(xmlStr);
		if (!data.containsKey(WXPayConstants.FIELD_SIGN)) {
			return false;
		}
		String sign = data.get(WXPayConstants.FIELD_SIGN);
		return generateSignature(data, key).equals(sign);
	}

	/**
	 * 判断签名是否正确，必须包含sign字段，否则返回false。使用MD5签名。
	 *
	 * @param data
	 *            Map类型数据
	 * @param key
	 *            API密钥
	 * @return 签名是否正确
	 * @throws Exception
	 */
	public static boolean isSignatureValid(Map<String, String> data, String key) throws Exception {
		return isSignatureValid(data, key, SignType.MD5);
	}

	/**
	 * 判断签名是否正确，必须包含sign字段，否则返回false。
	 *
	 * @param data
	 *            Map类型数据
	 * @param key
	 *            API密钥
	 * @param signType
	 *            签名方式
	 * @return 签名是否正确
	 * @throws Exception
	 */
	public static boolean isSignatureValid(Map<String, String> data, String key, SignType signType) throws Exception {
		if (!data.containsKey(WXPayConstants.FIELD_SIGN)) {
			return false;
		}
		String sign = data.get(WXPayConstants.FIELD_SIGN);
		return generateSignature(data, key, signType).equals(sign);
	}

	/**
	 * 生成签名
	 *
	 * @param data
	 *            待签名数据
	 * @param key
	 *            API密钥
	 * @return 签名
	 */
	public static String generateSignature(final Map<String, String> data, String key) throws Exception {
		return generateSignature(data, key, SignType.MD5);
	}

	/**
	 * 生成签名. 注意，若含有sign_type字段，必须和signType参数保持一致。
	 *
	 * @param data
	 *            待签名数据
	 * @param key
	 *            API密钥
	 * @param signType
	 *            签名方式
	 * @return 签名
	 */
	public static String generateSignature(final Map<String, String> data, String key, SignType signType)
			throws Exception {
		Set<String> keySet = data.keySet();
		String[] keyArray = keySet.toArray(new String[keySet.size()]);
		Arrays.sort(keyArray);
		StringBuilder sb = new StringBuilder();
		for (String k : keyArray) {
			if (k.equals(WXPayConstants.FIELD_SIGN)) {
				continue;
			}
			if (data.get(k).trim().length() > 0) // 参数值为空，则不参与签名
				sb.append(k).append("=").append(data.get(k).trim()).append("&");
		}
		sb.append("key=").append(key);
		if (SignType.MD5.equals(signType)) {
			return MD5(sb.toString()).toUpperCase();
		} else if (SignType.HMACSHA256.equals(signType)) {
			return HMACSHA256(sb.toString(), key);
		} else {
			throw new Exception(String.format("Invalid sign_type: %s", signType));
		}
	}

	/**
	 * 获取随机字符串 Nonce Str
	 *
	 * @return String 随机字符串
	 */
	public static String generateNonceStr() {
		char[] nonceChars = new char[32];
		for (int index = 0; index < nonceChars.length; ++index) {
			nonceChars[index] = SYMBOLS.charAt(RANDOM.nextInt(SYMBOLS.length()));
		}
		return new String(nonceChars);
	}

	/**
	 * 生成 MD5
	 *
	 * @param data
	 *            待处理数据
	 * @return MD5结果
	 */
	public static String MD5(String data) throws Exception {
		java.security.MessageDigest md = MessageDigest.getInstance("MD5");
		byte[] array = md.digest(data.getBytes("UTF-8"));
		StringBuilder sb = new StringBuilder();
		for (byte item : array) {
			sb.append(Integer.toHexString((item & 0xFF) | 0x100).substring(1, 3));
		}
		return sb.toString().toUpperCase();
	}

	/**
	 * 生成 HMACSHA256
	 * 
	 * @param data
	 *            待处理数据
	 * @param key
	 *            密钥
	 * @return 加密结果
	 * @throws Exception
	 */
	public static String HMACSHA256(String data, String key) throws Exception {
		Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
		SecretKeySpec secret_key = new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA256");
		sha256_HMAC.init(secret_key);
		byte[] array = sha256_HMAC.doFinal(data.getBytes("UTF-8"));
		StringBuilder sb = new StringBuilder();
		for (byte item : array) {
			sb.append(Integer.toHexString((item & 0xFF) | 0x100).substring(1, 3));
		}
		return sb.toString().toUpperCase();
	}

	/**
	 * 日志
	 * 
	 * @return
	 */
	public static Logger getLogger() {
		Logger logger = LoggerFactory.getLogger("wxpay java sdk");
		return logger;
	}

	/**
	 * 获取当前时间戳，单位秒
	 * 
	 * @return
	 */
	public static long getCurrentTimestamp() {
		return System.currentTimeMillis() / 1000;
	}

	/**
	 * 获取当前时间戳，单位毫秒
	 * 
	 * @return
	 */
	public static long getCurrentTimestampMs() {
		return System.currentTimeMillis();
	}

	/**
	 * 返回当前时间增加N分后的时间
	 * 
	 * @param time
	 *            时间，格式：yyyy-MM-dd HH:mm:ss
	 * @param n
	 *            分钟数
	 * @return yyyyMMddHHmmss
	 */
	public static String getNSecondTime(String time, int n) {

		if (time.isEmpty() || time == null) {
			return null;
		}
		Calendar c = Calendar.getInstance();
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			c.setTime(df.parse(time));
		} catch (ParseException e) {
			c.setTime(new Date());
		}
		c.add(Calendar.MINUTE, n);
		return df.format(c.getTime());
	}

	/**
	 * 格式化时间
	 * 
	 * @param time
	 *            时间，格式：yyyy-MM-dd HH:mm:ss
	 * @return yyyyMMddHHmmss
	 */
	public static String getFormatTime(String time) {

		if (time.isEmpty() || time == null) {
			return null;
		}
		time = time.replace(":", "").replace("-", "").replace(" ", "");
		return time;
	}
	
	/**
	 * 获取本地IP
	 * 
	 * @return
	 */
	public static String getLocalAddress(){
		String ip = "";
		try {
			ip = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ip;
	}
	
	/**
	 * 获取外网IP4地址
	 * 
	 * @return
	 */
	public static String getV4IP(){
		String ip = "";
		String chinaz = "http://ip.chinaz.com";
		
		StringBuilder inputLine = new StringBuilder();
		String read = "";
		URL url = null;
		HttpURLConnection urlConnection = null;
		BufferedReader in = null;
		try {
			url = new URL(chinaz);
			urlConnection = (HttpURLConnection) url.openConnection();
		    in = new BufferedReader( new InputStreamReader(urlConnection.getInputStream(),"UTF-8"));
			while((read=in.readLine())!=null){
				inputLine.append(read+"\r\n");
			}
			//System.out.println(inputLine.toString());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			if(in!=null){
				try {
					in.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		
		Pattern p = Pattern.compile("\\<dd class\\=\"fz24\">(.*?)\\<\\/dd>");
		Matcher m = p.matcher(inputLine.toString());
		if(m.find()){
			String ipstr = m.group(1);
			ip = ipstr;
			//System.out.println(ipstr);
		}
		return ip;
	}
	
}

package com.pay.wxpay.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import net.logstash.logback.encoder.org.apache.commons.io.IOUtils;

public class WxPayConfigImpl extends WXPayConfig{

	// 设置应用Id
	public static String appid = "2019102168481752";
	// 设置商户号
	public static String mch_id = "1230000109";
	// 设置设备号（终端设备号(门店号或收银设备ID)，默认请传"WEB"，非必需）
	public static String device_info = "WEB";
	// 设置字符集
	public static String key = "192006250b4c09247ec02edce69f6a2d";
	
	private static WxPayConfigImpl INSTANCE;
	
	public static WxPayConfigImpl getInstance() throws Exception {
        if (INSTANCE == null) {
            synchronized (WxPayConfigImpl.class) {
                if (INSTANCE == null) {
                    INSTANCE = new WxPayConfigImpl();
                }
            }
        }
        return INSTANCE;
    }
	
	@Override
	public String getAppID() {
		return appid;
	}

	@Override
	public String getMchID() {
		return mch_id;
	}

	@Override
	public String getKey() {
		return key;
	}

	@Override
	public InputStream getCertStream() {
		String fileUrl = "证书路径";
		InputStream certStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileUrl);
		byte[] certData = null;
		try {
			certData = IOUtils.toByteArray(certStream);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			if(certStream != null){
				try {
					certStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return new ByteArrayInputStream(certData);
	}

	@Override
	public IWXPayDomain getWXPayDomain() {
 		return WxPayDomainImpl.instance();
	}
}

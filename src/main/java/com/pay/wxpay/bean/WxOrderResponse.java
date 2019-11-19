package com.pay.wxpay.bean;

/**
 * 响应回调给APP参数
 */
public class WxOrderResponse {

	/**
	 * 应用ID
	 */
	private String appid;
	/**
	 * 商户号
	 */
	private String partnerid;
	/**
	 * 预支付交易会话ID
	 */
	private String prepayid;
	/**
	 * 扩展字段
	 */
	private String pack;
	/**
	 * 随机字符串
	 */
	private String noncestr;
	/**
	 * 时间戳
	 */
	private String timestamp;
	/**
	 * 签名
	 */
	private String sign;
	/**
	 * 错误代码
	 */
	private String err_code;
	/**
	 * 错误代码描述
	 */
	private String err_code_des;

	public String getAppid() {
		return appid;
	}

	public void setAppid(String appid) {
		this.appid = appid;
	}

	public String getPartnerid() {
		return partnerid;
	}

	public void setPartnerid(String partnerid) {
		this.partnerid = partnerid;
	}

	public String getPrepayid() {
		return prepayid;
	}

	public void setPrepayid(String prepayid) {
		this.prepayid = prepayid;
	}

	public String getPack() {
		return pack;
	}

	public void setPack(String pack) {
		this.pack = pack;
	}

	public String getNoncestr() {
		return noncestr;
	}

	public void setNoncestr(String noncestr) {
		this.noncestr = noncestr;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public String getSign() {
		return sign;
	}

	public void setSign(String sign) {
		this.sign = sign;
	}

	public String getErr_code() {
		return err_code;
	}

	public void setErr_code(String err_code) {
		this.err_code = err_code;
	}

	public String getErr_code_des() {
		return err_code_des;
	}

	public void setErr_code_des(String err_code_des) {
		this.err_code_des = err_code_des;
	}

}

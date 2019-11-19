package com.pay.wxpay;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.pay.wxpay.bean.WxOrderResponse;
import com.pay.wxpay.enums.WxAppPayResponseCode;
import com.pay.wxpay.utils.WXPay;
import com.pay.wxpay.utils.WXPayConstants;
import com.pay.wxpay.utils.WXPayUtil;
import com.pay.wxpay.utils.WxPayConfigImpl;

/**
 * 订单--统一下单/调起支付接口/支付结果通知/查询订单/关闭订单
 * 借鉴：https://blog.csdn.net/asd54090/article/details/81028323
 */
public class WxAppPayRequest {

	private static final Logger logger = LoggerFactory.getLogger(WxAppPayRequest.class);

	private WxPayConfigImpl config;
	private WXPay wxpay;

	/**
	 * 微信支付请求
	 */
	public WxAppPayRequest() {
		try {
			config = WxPayConfigImpl.getInstance();
			wxpay = new WXPay(config);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("微信配置初始化错误", e);
		}
	}

	/**
	 * APP支付订单请求
	 * 
	 * @param body
	 *            格式：APP名字-实际商品名称，如：天天爱消除-游戏充值
	 * @param attach
	 *            附加数据，在查询API和支付通知中原样返回
	 * @param outTradeNo
	 *            商户订单号
	 * @param totalFee
	 *            总金额
	 * @param startTime
	 *            订单开始时间String格式： yyyy-MM-dd HH:mm:ss
	 * @param expireMinute
	 *            有效时间（分钟）
	 * @param notifyUrl
	 *            微信支付异步通知回调地址
	 * @return
	 */
	private WxAppPayResponseCode getOrderSign(String body, String attach, String outTradeNo, BigDecimal totalFee,
			String startTime, int expireMinute, String notifyUrl) {

		// 准备好请求参数
		Map<String, String> map = new HashMap<String, String>();
		map.put("appid", WxPayConfigImpl.appid);
		map.put("mch_id", WxPayConfigImpl.mch_id);
		map.put("device_info", WxPayConfigImpl.device_info);
		map.put("nonce_str", WXPayUtil.generateNonceStr());
		map.put("sign_type", WXPayConstants.HMACSHA256);
		map.put("body", body);
		if (attach != null && !attach.isEmpty()) {
			map.put("attach", attach);
		}
		map.put("out_trade_no", outTradeNo);
		map.put("total_fee", totalFee.toString());
		map.put("spbill_create_ip", WXPayUtil.getLocalAddress());
		map.put("time_start", WXPayUtil.getFormatTime(startTime));
		String endTime = WXPayUtil.getNSecondTime(startTime, expireMinute);
		map.put("time_expire", WXPayUtil.getFormatTime(endTime));
		map.put("notify_url", notifyUrl);
		map.put("trade_type", "APP");

		// 生成带sign的xml字符串
		Map<String, String> unifiedOrderMap = null;
		try {
			unifiedOrderMap = wxpay.unifiedOrder(map);
			if (unifiedOrderMap == null || (unifiedOrderMap != null && "FAIL".equals(unifiedOrderMap.get("return_code")))) {
				String errorMsg = "调用微信“统一下单”获取prepayid 失败...";
				logger.info("getOrderSign --unifiedOrder: 调用微信“统一下单”获取prepayid 失败.");
				logger.info("getOrderSign --unifiedOrder: 请求参数：" + map.toString());
				logger.info("getOrderSign --unifiedOrder: 返回Map：" + unifiedOrderMap);
				if(unifiedOrderMap != null){
					errorMsg += " 异常信息为：" + unifiedOrderMap.get("return_msg");
				}
				WxAppPayResponseCode error = WxAppPayResponseCode.ERROR;
				error.setAlias(errorMsg);
				return error;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error("getOrderSign : 调用微信“统一下单”失败 。", e);
			WxAppPayResponseCode error = WxAppPayResponseCode.ERROR;
			error.setAlias("调用微信“统一下单”失败 。" + e.toString());
			return error;
		}
		
		//调用微信请求成功，但响应失败
		String resultCode = unifiedOrderMap.get("result_code");
		if(WxAppPayResponseCode.FAIL.code().equals(resultCode)){
			WxAppPayResponseCode error = WxAppPayResponseCode.findCode(unifiedOrderMap.get("err_code"));
			return error;
		}

		return parseWXOrderResponse(unifiedOrderMap);
	}

	/**
	 * 将map转成客户端订单用的封装体
	 *
	 * @param map
	 *            map
	 * @return 用户端用的封装体
	 */
	private WxAppPayResponseCode parseWXOrderResponse(Map<String, String> map) {

		WxOrderResponse response = new WxOrderResponse();
		response.setAppid(map.get("appid"));
		response.setPartnerid(map.get("partnerid"));
		response.setPrepayid(map.get("prepay_id"));
		response.setPack("Sign=WXPay");
		response.setNoncestr(map.get("noncestr"));
		String timestamp = WXPayUtil.getCurrentTimestamp() + "";
		response.setTimestamp(timestamp);

		// 前人踩坑，咱们乘凉
		// sgin（签名），不是拿微信“统一下单”返回的sgin，而是自己再签一次，返回给客户端
		// 签名的参数拿的不是“统一下单”，而是拿“调起支付接口”里面的参数，这步API文档写的是客户端生成，不过咱们服务端就帮他做了
		// 注意：map的key不能是大写
		Map<String, String> params = new HashMap<>();
		params.put("appid", map.get("appid"));
		params.put("partnerid", map.get("partnerid"));
		params.put("prepayid", map.get("prepay_id"));
		params.put("package", "Sign=WXPay");
		params.put("noncestr", map.get("nonce_str"));
		params.put("timestamp", timestamp);
		try {
			// 这个sign是移动端要请求微信服务端的，也是我们要保存后面校验的
			String sgin = WXPayUtil.generateSignature(params, config.getKey());
			response.setSign(sgin);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("parseWXOrderResponse : 订单第二次供客户端签名信息失败 。");
			logger.error("parseWXOrderResponse : 请求参数：" + params.toString());
			logger.error("parseWXOrderResponse : 返回错误信息：", e);
			WxAppPayResponseCode errorData = WxAppPayResponseCode.ERROR;
			errorData.setAlias("调用支付接口生成签名sign失败！");
			return errorData;
		}
		WxAppPayResponseCode successData = WxAppPayResponseCode.SUCCESS;
		successData.setAlias(JSONObject.toJSONString(response));
		return successData;
	}
	

	/**
	 * 查微信订单
	 *
	 * @param outTradeNo
	 *            订单号
	 */
	public WxAppPayResponseCode queryOrderByOutTradeNo(String outTradeNo) {

		logger.info("查询微信支付订单信息，订单号为：" + outTradeNo);
		HashMap<String, String> data = new HashMap<String, String>();
		data.put("out_trade_no", outTradeNo);
		try {
			Map<String, String> orderQueryMap = wxpay.orderQuery(data);
			WxAppPayResponseCode successData = WxAppPayResponseCode.SUCCESS;
			successData.setAlias(JSONObject.toJSONString(orderQueryMap));
			return successData;
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("queryOrderByOutTradeNo : 查询微信订单支付信息失败 。订单号：" + outTradeNo);
			logger.error("queryOrderByOutTradeNo : 返回错误信息：", e);
			WxAppPayResponseCode errorData = WxAppPayResponseCode.ERROR;
			errorData.setAlias("调用查微信订单支付信息接口失败！");
			return errorData;
		}
	}
	
	/**
     * 关闭订单
     *
     * @param outTradeNo 订单号
     * @return
     */
    public WxAppPayResponseCode closeOrder(String outTradeNo) {
    	
    	logger.info("关闭微信支付订单信息，订单号为：" + outTradeNo);
        HashMap<String, String> data = new HashMap<>();
        data.put("out_trade_no", outTradeNo);
        try {
            Map<String, String> closeOrderMap = wxpay.closeOrder(data);
			WxAppPayResponseCode successData = WxAppPayResponseCode.SUCCESS;
			successData.setAlias(JSONObject.toJSONString(closeOrderMap));
			return successData;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("closeOrder : 微信关闭订单失败 。订单号：" + outTradeNo);
			logger.error("closeOrder : 返回错误信息：", e);
			WxAppPayResponseCode errorData = WxAppPayResponseCode.ERROR;
			errorData.setAlias("调用查微信订单支付信息接口失败！");
			return errorData;
        }
    }

	/**
	 * 是否成功接收微信支付回调 用于回复微信，否则微信回默认为商户后端没有收到回调
	 *
	 * @return
	 */
	public String returnWXPayVerifyMsg() {
		return "<xml>\n" + "\n" + "  <return_code><![CDATA[SUCCESS]]></return_code>\n"
				+ "  <return_msg><![CDATA[OK]]></return_msg>\n" + "</xml>";
	}

	public static void main(String[] args) {

		WxAppPayRequest pay = new WxAppPayRequest();
		WxAppPayResponseCode orderSign = pay.getOrderSign("APP-商品订单支付", "data1;data2", "212458542512542542",
				new BigDecimal("100.12"), "2019-01-02 23:55:14", 10, "http://XXX");
		System.out.println(orderSign);
	}

}

package com.pay.wxpay.enums;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 微信查询订单支付状态
 */
public enum WxAppPayNotifyDataTradeStateEnum {

	SUCCESS("SUCCESS", "支付成功"), 
	REFUND("REFUND", "转入退款"),
	NOTPAY("NOTPAY", "未支付"),
	CLOSED("CLOSED", "已关闭"),
	REVOKED("REVOKED", "已撤销（刷卡支付）"),
	USERPAYING("USERPAYING", "用户支付中"),
	PAYERROR("PAYERROR", "支付失败(其他原因，如银行返回失败)");

	private String value;
	private String alias;

	private WxAppPayNotifyDataTradeStateEnum(String value, String alias) {
		this.value = value;
		this.alias = alias;
	}

	public String value() {
		return value;
	}

	public String alias() {
		return alias;
	}
	
	/**
	 * 获取枚举值列表
	 * 
	 * @return
	 */
	public static List<Map<String, Object>> list() {
		
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		WxAppPayNotifyDataTradeStateEnum[] values = WxAppPayNotifyDataTradeStateEnum.values();
		for (WxAppPayNotifyDataTradeStateEnum tempEnum : values) {
			Map<String,Object> map = new HashMap<String, Object>();
			map.put("value", tempEnum.value());
			map.put("alias", tempEnum.alias());
			list.add(map);
		}
		return list;
	}
	
	/**
	 * 通过value获取alias
	 * 
	 * @param value
	 * @return
	 */
	public static String findAliasByValue(String value) {
		
		WxAppPayNotifyDataTradeStateEnum[] values = WxAppPayNotifyDataTradeStateEnum.values();
		for (WxAppPayNotifyDataTradeStateEnum tempEnum : values) {
			if(value.equals(tempEnum.value())){
				return tempEnum.alias();
			}
		}
		return "";
	}
}

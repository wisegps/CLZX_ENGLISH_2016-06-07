package com.wisegps.clzx.util;


import org.json.JSONArray;

import android.content.Context;

import com.wisegps.clzx.R;


public class ResolveData {

	/**
	 * 获取车辆图标的4中状态
	 * 
	 * @param Rcv_time
	 * @param jsonArray
	 * @param speed
	 * @return 1 离线；2 报警；3 行驶；4 静止
	 */
	
	public static int getCarStatus(Context context,String Rcv_time, JSONArray jsonArray,
			int speed) {
		if (AllStaticClass.GetTimeDiff(Rcv_time) > 10) {
			return 1;
		} else if (getUniAlertsDesc(context,jsonArray).length() > 0) {
			return 2;
		} else if (speed > 0) {
			return 3;
		} else {
			return 4;
		}
	}

	
	/**
	 * 轨迹播放判断当前状态，不判断离线
	 * 
	 *            gps时间
	 * @param gps_flag
	 *            flag
	 * @param speed
	 *            速度
	 * @param UniStatusDesc
	 *            状态
	 * @param UniAlertsDesc
	 *            状态
	 * @return
	 */
	public static String getStatusDesc(Context context,int gps_flag,
			int speed, String UniStatusDesc, String UniAlertsDesc) {
		String desc = "";
			if (gps_flag % 2 == 0) {
				if (speed > 10) {// 速度判断
					desc = context.getResources().getString(R.string.car_drive) + UniStatusDesc + UniAlertsDesc + " " + speed
							+ context.getResources().getString(R.string.speed_kmh);
				} else {
					desc = context.getResources().getString(R.string.car_stop) + UniStatusDesc + UniAlertsDesc;
				}
			} else {
				if (speed > 10) {
					desc = context.getResources().getString(R.string.car_Blind) + UniStatusDesc + UniAlertsDesc;
				} else {
					desc = context.getResources().getString(R.string.car_stop) + UniStatusDesc + UniAlertsDesc;
				}
			}
		if (desc.endsWith(",")) {// 格式化结果
			desc = desc.substring(0, desc.length() - 1);
		}
		return desc;
	}
	
	/**
	 * 判断当前状态
	 * 
	 * @param Gps_time
	 *            gps时间
	 * @param gps_flag
	 *            flag
	 * @param speed
	 *            速度
	 * @param UniStatusDesc
	 *            状态
	 * @param UniAlertsDesc
	 *            状态
	 * @return
	 */
	public static String getStatusDesc(Context context,String Rec_time, int gps_flag,
			int speed, String UniStatusDesc, String UniAlertsDesc) {
		String desc = "";
		long time = GetSystem.GetTimeDiff(Rec_time);
		if (time < 10) {// 是否在线
			if (gps_flag % 2 == 0) {
				if (speed > 10) {// 速度判断
					desc = context.getResources().getString(R.string.car_drive) + UniStatusDesc + UniAlertsDesc + " " + speed
							+ context.getResources().getString(R.string.speed_kmh);
				} else {
					desc = context.getResources().getString(R.string.car_stop) + UniStatusDesc + UniAlertsDesc;
				}
			} else {
				if (speed > 10) {
					desc = context.getResources().getString(R.string.car_Blind) + UniStatusDesc + UniAlertsDesc;//"盲区,"
				} else {
					desc = context.getResources().getString(R.string.car_stop) + UniStatusDesc + UniAlertsDesc;
				}
			}
		} else {
			desc = context.getResources().getString(R.string.car_offline) + GetSystem.ShowOfflineTime(time);//离线
		}
		if (desc.endsWith(",")) {// 格式化结果
			desc = desc.substring(0, desc.length() - 1);
		}
		return desc;
	}

	static String STATUS_FORTIFY = "8193";
	static String STATUS_LOCK = "8194";
	static String STATUS_NETLOC = "8195";
	static String STATUS_SLEEP = "8197";

	static int STATUS_RUN = 8196;

	public static String getUniStatusDesc(Context context,JSONArray jsonArray) {
		String str = "";
		for (int i = 0; i < jsonArray.length(); i++) {
			try {
				String jsonString = jsonArray.getString(i);
				if (jsonString.equals(STATUS_FORTIFY)) {
					str +=  context.getResources().getString(R.string.car_set_fortification);//"设防,"
				} else if (jsonString.equals(STATUS_LOCK)) {
					str +=  context.getResources().getString(R.string.car_Lock_the_car);//"锁车,"
				} else if (jsonString.equals(STATUS_FORTIFY)) {
					str += context.getResources().getString(R.string.car_base_station_location);// "基站定位," 
				} else if (jsonString.equals(STATUS_FORTIFY)) {
					str +=  context.getResources().getString(R.string.car_energy_saver_mode);//"省电状态,"
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return str;
	}

	public static String getRunStopDesc(Context context,int[] status,String lastStopTime) {
		
		String str = context.getResources().getString(R.string.accoff)  + " " + GetSystem.getStopDuration(lastStopTime);
		if(status == null){
			return "";
		}
		for (int i = 0; i < status.length; i++) {
			int s = status[i];
			if (s == STATUS_RUN) {
				str = context.getResources().getString(R.string.accon) + " ";
				break;
			} 
		}
		
		return str;
	}
	
	/**
	 * 是否包含运行状态
	 * @param status
	 * @return
	 */
	public static boolean hasStatusRun(int[] status) {
		
		boolean b = false;
		if(status == null || status.length == 0){
			return b;
		}
		for (int i = 0; i < status.length; i++) {
			int s = status[i];
			if (s == STATUS_RUN) {
				b = true;
				break;
			} 
		}
		return b;
	}
	
	static String ALERT_SOS = "12289";
	static String ALERT_OVERSPEED = "12290";
	static String ALERT_VIRBRATE = "12291";
	static String ALERT_MOVE = "12292";
	static String ALERT_ALARM = "12293";
	static String ALERT_INVALIDRUN = "12294";
	static String ALERT_ENTERGEO = "12295";
	static String ALERT_EXITGEO = "12296";
	static String ALERT_CUTPOWER = "12297";
	static String ALERT_LOWPOWER = "12298";
	static String ALERT_GPSCUT = "12299";
	static String ALERT_OVERDRIVE = "12300";
	static String ALERT_INVALIDACC = "12301";
	static String ALERT_INVALIDDOOR = "12302";

	public static String getUniAlertsDesc(Context context ,JSONArray jsonArray) {
		String str = "";
		for (int i = 0; i < jsonArray.length(); i++) {
			try {
				String jsonString = jsonArray.getString(i);
				if (jsonString.equals(ALERT_SOS)) {
					str +=  context.getResources().getString(R.string.car_emergency_alarm);//"紧急报警,"
				} else if (jsonString.equals(ALERT_OVERSPEED)) {
					str +=  context.getResources().getString(R.string.car_speed_alarm);//"超速报警,"
				} else if (jsonString.equals(ALERT_VIRBRATE)) {
					str += context.getResources().getString(R.string.car_vibration_alarm);//"震动报警,"
				} else if (jsonString.equals(ALERT_MOVE)) {
					str += context.getResources().getString(R.string.car_displacement_alarm);//"位移报警,"
				} else if (jsonString.equals(ALERT_ALARM)) {
					str += context.getResources().getString(R.string.car_burglar_alarm);//"防盗器报警,"
				} else if (jsonString.equals(ALERT_INVALIDRUN)) {
					str += context.getResources().getString(R.string.car_illegal_driving_alarm);//"非法行驶报警,
				} else if (jsonString.equals(ALERT_ENTERGEO)) {
					str += context.getResources().getString(R.string.car_into_fence_alarm);//"进围栏报警,"
				} else if (jsonString.equals(ALERT_EXITGEO)) {
					str +=context.getResources().getString(R.string.car_out_of_fence_alarm);// "出围栏报警,"
				} else if (jsonString.equals(ALERT_CUTPOWER)) {
					str += context.getResources().getString(R.string.car_cut_off_line_alarm);//"剪线报警,"
				} else if (jsonString.equals(ALERT_LOWPOWER)) {
					str += context.getResources().getString(R.string.car_low_power_alarm);//"低电压报警,"
				} else if (jsonString.equals(ALERT_GPSCUT)) {
					str += context.getResources().getString(R.string.car_GPS_disconnection_alarm);//"GPS断线报警,"
				} else if (jsonString.equals(ALERT_OVERDRIVE)) {
					str += context.getResources().getString(R.string.car_fatigue_driving_alarm);//"疲劳驾驶报警,"
				} else if (jsonString.equals(ALERT_INVALIDACC)) {
					str += context.getResources().getString(R.string.car_illegal_power_alarm);//"非法点火报警,"
				} else if (jsonString.equals(ALERT_INVALIDDOOR)) {
					str += context.getResources().getString(R.string.car_illegal_door_alarm);//"非法开门报警,"
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return str;
	}
}
package com.wisegps.clzx.view;

import java.util.ArrayList;
import java.util.List;

import android.WGoogleMap;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.wisegps.clzx.R;
import com.wisegps.clzx.activity.MainActivity;
import com.wisegps.clzx.app.Msg;
import com.wisegps.clzx.biz.MapBiz;
import com.wisegps.clzx.entity.CarInfo;
import com.wisegps.clzx.entity.ContacterData;
import com.wisegps.clzx.util.AllStaticClass;
import com.wisegps.clzx.util.GetSystem;
import com.wisegps.clzx.util.ResolveData;
import com.wisegps.clzx.util.TimeFactory;
import com.wisegps.clzx.view.adapter.OnCarAutoSelectedListener;
import com.wisegps.clzx.view.adapter.OnCarSelectedByKeyListener;
import com.wisegps.clzx.view.adapter.OnClickTimeDialogListener;

/**
 * @author Wu
 * googleMap
 */
public class GoogleMapViewHelper implements OnClickTimeDialogListener {

	private Context mContext;
	private float zoom = 16;
	private GoogleMap googleMap;
	private WGoogleMap wGoogleMap;
	private MapPopupWindow menuWindow;
	private View popParkView;// 停车标识弹出信息
	private View popCarView;// 停车标识弹出信息
	
	private List<Marker> carMakerList = new ArrayList<Marker>();// 车辆标记
	private List<Marker> parkMakerList = new ArrayList<Marker>();// 停车标记
	private List<CarInfo> carInfoList = new ArrayList<CarInfo>();// 车辆信息
	
	private List<Bundle> bundleList = new ArrayList<Bundle>();
	

	private CarInfo lastCarPath;// 上一次轨迹点
	private TimeDialog timeDialog = null;// 轨迹回放时间选择框
	private int currentCarIndex = 0; // 当前显示车辆在车辆信息列表总的位置
	private Bitmap bitmapParking = null;// 停车图标位图
	private MapBiz mapBiz;// 地图页面管理类，负责网络请求，解析等数据处理
	private TrackView trackView;// 轨迹回放，相关业务操作
	private FlowView flowView;// 跟踪相关业务操作
	private TextView tvAddress = null;// 底部显示当前车辆地址

	// 点击地图窗口，根据title判断点击的是车辆图标，还是停车图标
	private String Car_Marker_Title = "car";
	private String Park_Marker_Title = "parking";
	private int clickMarkerIndex = 0;
	private CarInfoAutoText carInfoAutoText;
	private OnCarSelectedByKeyListener onCarSelectedByKeyListener;
	
	public GoogleMapViewHelper(Context context,GoogleMap googleMap){
		this.mContext = context;
		this.googleMap = googleMap;
		wGoogleMap = new WGoogleMap(context,googleMap);
		this.googleMap.setOnMarkerClickListener(onMarkerClickListener);
		this.mapBiz = new MapBiz(handler,context);
		this.trackView = new TrackView(context, handler);
		this.flowView = new FlowView(context, handler);
		// 实现OnClickTimeDialogListener接口，在time函数返回时间
		this.timeDialog = new TimeDialog(mContext, this);
		tvAddress = (TextView) ((Activity) context).findViewById(R.id.tv_address);
		bitmapParking = BitmapFactory.decodeResource(context.getResources(),
				R.drawable.park);
		OnCarAutoSelectedListener onCarAutoSelectedListener = new OnCarAutoSelectedListener(){

			@Override
			public void onCarAutoSelected(CarInfo carInfo) {
				onCarSelectedByKeyListener.onCarSelected(carInfo);
			}
		};
		//车辆搜索框
		carInfoAutoText = new CarInfoAutoText(context,R.id.et_MapSearch,onCarAutoSelectedListener);
	}
	
	/**
	 * Handler
	 */
	private Handler handler = new Handler() {
		@SuppressLint("HandlerLeak") @Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case Msg.Track_Click:// 轨迹回放
				timeDialog.showDialog();
				lastCarPath = null;
				break;
			case Msg.Track_Flow:// 跟踪
				lastCarPath = null;
				int cindex = currentCarIndex;
				reset();
				currentCarIndex = cindex;
				showTrackCarMarker(currentCarIndex);
				flowView.start(carInfoList.get(currentCarIndex));
				break;
			case Msg.Track_Draw_Line:
				Log.i("MapViewHelper", "Track_Draw_Line"+TimeFactory.getCurrentStringTime());
				Bundle bundle = msg.getData();
				CarInfo carPath = (CarInfo) bundle.getSerializable("CarPath");
				drawLine(carPath);
				break;
			case Msg.Track_Stop:
				Log.i("MapViewHelper", "Track_Stop"+TimeFactory.getCurrentStringTime());
				int index = currentCarIndex;
				reset();
				currentCarIndex = index;
				showAllCar(carInfoList, currentCarIndex);
				break;
			case Msg.Track_Is_Null:
				break;
			case Msg.Track_Not_Null:
				clearCarMarkerList();
				clearParkCarMarkerList();
				wGoogleMap.clear();
				lastCarPath = null;
				break;
			case Msg.Notice_Flow_Stop:
				index = currentCarIndex;
				reset();
				currentCarIndex = index;
				showAllCar(carInfoList, currentCarIndex);
				break;
			case Msg.Notice_Track_Stop:
				index = currentCarIndex;
				reset();
				currentCarIndex = index;
				break;
			case Msg.Call_Phone:
				String tel = (String) msg.obj;
				try {// 平板没有电话模块异常
					Intent callIntent = new Intent(Intent.ACTION_DIAL,
							Uri.parse("tel:" + tel ));
					mContext.startActivity(callIntent);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			}
		}
	};

	/**
	 * 显示所有汽车标注
	 * 
	 * @param carinfos
	 * @param index
	 */
	public void showAllCar(List<CarInfo> carinfos, int index) {
		//汽车搜索框，设置用户信息
		ContacterData contacterData = ((MainActivity) mContext).getCurrentUserInfo();
		carInfoAutoText.setContacterData(contacterData);
		Log.i("MapViewHelper", "显示所有汽车标注" + carinfos.size());
		reset();
		this.carInfoList = carinfos;
		for (int i = 0; i < carinfos.size(); i++) {
			showCarOnMap(i);
		}
		setFocusCar(index);
	}
	
	
	/**
	 * @param carinfos
	 */
	public void refreshCar(List<CarInfo> carinfos) {
		//Log.i("MapViewHelper", "刷新前汽车数量" + this.carInfoList.size());
		int index = this.currentCarIndex;
		reset();
		this.carInfoList = carinfos;
		//Log.i("MapViewHelper", "刷新后汽车数量" + this.carInfoList.size());
		for (int i = 0; i < carinfos.size(); i++) {
			showCarOnMap(i);
		}
		showMarkWindow(currentCarIndex);
		// 定义用于显示该InfoWindow的坐标点
		String Lat = carInfoList.get(index).getLat();
		String Lon = carInfoList.get(index).getLon();
		double lat = Float.valueOf(Lat);
		double lon = Float.valueOf(Lon);
		LatLng point = new LatLng(lat, lon);
		wGoogleMap.getAddress(point,tvAddress);
	}
	
	/**
	 * 根据车辆序号在地图上显示车辆，重载showCarOnMap
	 */
	public void showCarOnMap(int i) {
		if(this.carInfoList.size()>0){
			CarInfo carInfo = this.carInfoList.get(i);
			showCarOnMap(carInfo, i);
		}
	}

	/**
	 * 在地图上显示车辆，重载showCarOnMap
	 */
	public void showCarOnMap(CarInfo carInfo, int index) {
		String Direct = carInfo.getDirect();
		int CarStatus = carInfo.getCarStatus();
		Bitmap carBitmap = AllStaticClass.getCarBimpMap(mContext, CarStatus,
				Direct);
		String Lat = carInfo.getLat();
		String Lon = carInfo.getLon();
		double lat = Float.valueOf(Lat);
		double lon = Float.valueOf(Lon);
		LatLng point = new LatLng(lat, lon);
		// 构建Marker图标
		BitmapDescriptor bitmap = BitmapDescriptorFactory.fromBitmap(carBitmap);
		MarkerOptions options = wGoogleMap.setMarkOptions(point, bitmap);
		Marker marker = (Marker) (wGoogleMap.addMarker(options));
		marker.setTitle(Car_Marker_Title);
		carMakerList.add(marker);
	}
	
	/**
	 * @param index
	 */
	public void setFocusCar(int index) {
		if((carInfoList.size()>0)){
			String Lat = this.carInfoList.get(index).getLat();
			String Lon = this.carInfoList.get(index).getLon();
			double lat = Float.valueOf(Lat);
			double lon = Float.valueOf(Lon);
			LatLng point = new LatLng(lat, lon);
			wGoogleMap.animateCamera(point, zoom, 0, 0);
			wGoogleMap.getAddress(point,tvAddress);
		}
	}

	
	/**
	 * @param index 在哪个makrer显示标签
	 */
	String managerPhone,drivePhone,managerName,driveName;
	public void showMarkWindow(final int index){
		currentCarIndex = index;
		final CarInfo carInfo = carInfoList.get(index);
		googleMap.setInfoWindowAdapter(new InfoWindowAdapter() {
			
			@Override
			public View getInfoWindow(Marker arg0) {
				// TODO Auto-generated method stub
				popCarView = mapBiz.getPopView(mContext, carInfo);
				managerName = carInfo.getManager();
				driveName   = carInfo.getDriver();
				drivePhone   = carInfo.getPhone();
				managerPhone = carInfo.getPhone1();;
				Log.d("CLZX_GOOGLE", "--22222--" + drivePhone + "  " + managerPhone);
				return popCarView;
			}
			
			@Override
			public View getInfoContents(Marker arg0) {
				// TODO Auto-generated method stub
				return null;
			}
		});
		googleMap.setOnInfoWindowClickListener(new OnInfoWindowClickListener() {
			
			@Override
			public void onInfoWindowClick(Marker marker) {
				/*marker.hideInfoWindow();*/
				menuWindow = new MapPopupWindow(mContext, onPopItemsClickListener);
                menuWindow.showAtLocation(popCarView,
                        Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0); 
			}
		});		
	}
	
	/**
	 * 画轨迹线
	 * 
	 * @param carPath
	 */
	public void drawLine(CarInfo carPath) {
		if(isRunning() == false){
			Log.i("MapViewHelper", "。。。停止画轨迹");
			return;
		}
		if (carPath == null) {
			Log.i("MapViewHelper", "坐标为空");
			return;
		}
		if (lastCarPath == null) {
			this.lastCarPath = carPath;
		}
		/** 获取上次坐标信息*/
		String lastStrLat = lastCarPath.getLat();
		String lastStrLon = lastCarPath.getLon();
		double lastLat = Float.valueOf(lastStrLat);
		double lastLon = Float.valueOf(lastStrLon);
		LatLng lastPoint = new LatLng(lastLat, lastLon);
		/** 本次车辆坐标信息*/
		String Lat = carPath.getLat();
		String Lon = carPath.getLon();
		double lat = Float.valueOf(Lat);
		double lon = Float.valueOf(Lon);
		LatLng point = new LatLng(lat, lon);
		List<LatLng> points = new ArrayList<LatLng>();
		points.add(lastPoint);// 点元素
		points.add(point);// 点元素
		wGoogleMap.getAddress(point,tvAddress);
		wGoogleMap.drawLine(points, Color.GREEN, 5);
		/*PolylineOptions polyline = new PolylineOptions().color(Color.GREEN).addAll(points);
		googleMap.addPolyline(polyline);*/
		// 添加到地图
		clearCarMarkerList();
		showCarOnMap(carPath);
        addParkOverlay(lastCarPath, carPath);
		wGoogleMap.animateCamera(point, 8, 0, 0);
		this.lastCarPath = carPath;
		Log.i("MapViewHelper", "画轨迹线时间"+TimeFactory.getCurrentStringTime());
		showPopup(carPath);
	}
	
	/**
	 * 显示车辆信息,车辆轨迹回放显示
	 * 
	 * @param index
	 */
	public void showPopup(final CarInfo carInfo) {
		final boolean isTrack = true;
		googleMap.setInfoWindowAdapter(new InfoWindowAdapter() {
			
			@Override
			public View getInfoWindow(Marker arg0) {
				// TODO Auto-generated method stub
				String name = carInfoList.get(currentCarIndex).getObj_name();
				popCarView = mapBiz.getPopView(mContext,name,carInfo,isTrack);
				return popCarView;
			}
			
			@Override
			public View getInfoContents(Marker arg0) {
				// TODO Auto-generated method stub
				return null;
			}
		});
	}
	
	/**
	 * 画轨迹时候在地图上显示车辆，重载showCarOnMap
	 */
	public void showCarOnMap(CarInfo carInfo) {
		showCarOnMap(carInfo, currentCarIndex);
	}
   /*
	* 清空汽车标注,回收标注图片内存
	*/
	public void clearCarMarkerList() {
		for (int i = 0; i < carMakerList.size(); i++) {
			carMakerList.get(i).remove();
		}
		carMakerList.clear();
	}
	
	
	/**
	 * 清空停车标志
	 */
	public void clearParkCarMarkerList(){
		for (int i = 0; i < parkMakerList.size(); i++) {
			parkMakerList.get(i).remove();
		}
		parkMakerList.clear();
		bundleList.clear();
	}
	
	/**
	 * 重置map
	 */
	public void reset() {
		trackView.reset();
		flowView.reset();
		clearCarMarkerList();
		clearParkCarMarkerList();
		googleMap.clear();
		currentCarIndex = 0;
		zoom = 16;
		this.lastCarPath = null;
		tvAddress.setText("");
	}
	
	public void stopTrack() {
		trackView.stop();
	}
	public void stopFlow() {
		flowView.stop();
	}
	
	public boolean isRunning() {
		return flowView.isRunning() | trackView.isRunning();
	}
	
	/**
	 * 汽车标注点击事件
	 */
	private OnMarkerClickListener onMarkerClickListener = new OnMarkerClickListener() {
		public boolean onMarkerClick(Marker marker) {
			String title = marker.getTitle();
			if (title.equals(Car_Marker_Title)) {
				for (int i = 0; i < carMakerList.size(); i++) {
					if(marker.equals(carMakerList.get(i))){
						Log.d("CLZX_GOOGLE", "--点击第几辆车--" + i);
						wGoogleMap.animateCamera(marker.getPosition(), 8, 0, 0);
						showMarkWindow(i);
					}
				}
			} else if (title.equals(Park_Marker_Title)) {
				for (int j = 0; j < parkMakerList.size(); j++) {
					if(marker.equals(parkMakerList.get(j))){
						Log.d("CLZX_GOOGLE", "--点击第几辆车--" + j);
						wGoogleMap.animateCamera(marker.getPosition(), 8, 0, 0);
						showPopParking(bundleList.get(j));
					}
				}	
			}
			wGoogleMap.getAddress(marker.getPosition(),tvAddress);
			return false;
		}
	};
	
	/**
	 * @param onCarSelectedByKeyListener
	 */
	public void setOnCarSelectByKeyListener(OnCarSelectedByKeyListener onCarSelectedByKeyListener){
		this.onCarSelectedByKeyListener = onCarSelectedByKeyListener;
	}
	
	/**
	 * popupwindow onclick
	 */
	private OnClickListener onPopItemsClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if(menuWindow!=null){
				menuWindow.dismiss();
			}
			switch (v.getId()) {
			
			case R.id.rl_manager_call:
				mapBiz.call(managerPhone);
				break;
			case R.id.rl_drive_call:
				mapBiz.call(drivePhone);			
				break;
			case R.id.rl_track_location:
				handler.sendEmptyMessage(Msg.Track_Flow);
				break;
			case R.id.rl_track_replay:
				handler.sendEmptyMessage(Msg.Track_Click);
				break;
			}
		}
	};
	
	/**
	 * @author Wu
	 * 
	 */
	class MapPopupWindow extends PopupWindow {

        private RelativeLayout rl_mana_call,rl_dri_call,rl_track_location,rl_track_replay;    
        private TextView tv_dismiss,tv_man_phone, tv_dri_phone;
        private View mMenuView;

        @SuppressLint("InflateParams") public MapPopupWindow(Context context, View.OnClickListener itemsOnClick) {

            LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            mMenuView = inflater.inflate(R.layout.item_map_popwindow, null); 
            rl_mana_call = (RelativeLayout)mMenuView.findViewById(R.id.rl_manager_call);
            rl_dri_call  = (RelativeLayout)mMenuView.findViewById(R.id.rl_drive_call);
            rl_track_location  = (RelativeLayout)mMenuView.findViewById(R.id.rl_track_location);
            rl_track_replay    = (RelativeLayout)mMenuView.findViewById(R.id.rl_track_replay);
            tv_man_phone = (TextView)mMenuView.findViewById(R.id.tv_manager_phone);
            tv_dri_phone = (TextView)mMenuView.findViewById(R.id.tv_drive_phone);
            tv_dismiss   = (TextView)mMenuView.findViewById(R.id.tv_dismiss);
            rl_mana_call.setOnClickListener(itemsOnClick);
            rl_dri_call.setOnClickListener(itemsOnClick);
            rl_track_location.setOnClickListener(itemsOnClick);
            rl_track_replay.setOnClickListener(itemsOnClick);
            
            if(!TextUtils.isEmpty(managerPhone)){
            	rl_mana_call.setVisibility(View.VISIBLE);
            	tv_man_phone.setText(context.getString(R.string.car_manager) + managerName + " " + managerPhone);
            }else{
            	rl_mana_call.setVisibility(View.GONE);
            }
            if(!TextUtils.isEmpty(drivePhone)){
            	rl_dri_call.setVisibility(View.VISIBLE);
            	tv_dri_phone.setText(context.getString(R.string.car_driver) + driveName + " " + drivePhone);
            }else{
            	rl_dri_call.setVisibility(View.GONE);
            }
            //关闭popupwindow
            tv_dismiss.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });
            //设置SelectPicPopupWindow的View
            this.setContentView(mMenuView);
            //设置SelectPicPopupWindow弹出窗体的宽
            this.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
            //设置SelectPicPopupWindow弹出窗体的高
            this.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
            //设置SelectPicPopupWindow弹出窗体可点击
            this.setFocusable(true);
            //设置SelectPicPopupWindow弹出窗体动画效果
            this.setAnimationStyle(R.style.popupwindow_buttom_anim);
            //实例化一个ColorDrawable颜色为半透明
//            ColorDrawable dw = new ColorDrawable(0xffffff);
//            设置SelectPicPopupWindow弹出窗体的背景
//            this.setBackgroundDrawable(dw); 
        }
    }
	
	/**
	 * @param index
	 */
	private void showTrackCarMarker(int index){
		String Lat = carInfoList.get(index).getLat();
		String Lon = carInfoList.get(index).getLon();
		double lat = Float.valueOf(Lat);
		double lon = Float.valueOf(Lon);
		LatLng point = new LatLng(lat, lon);
		wGoogleMap.addMarker(point, "");
	}

	/**
	 * 显示车辆停留信息
	 * 
	 * @param index
	 */
	public void showPopParking(final Bundle bundle) {
		googleMap.setInfoWindowAdapter(new InfoWindowAdapter() {
			
			@Override
			public View getInfoWindow(Marker arg0) {
				popParkView = mapBiz.getPopParkingView(mContext, bundle);
				// 定义用于显示该InfoWindow的坐标点
				String Lat = bundle.getString("Lat");
				String Lon = bundle.getString("Lon");
				double lat = Float.valueOf(Lat);
				double lon = Float.valueOf(Lon);
				LatLng point = new LatLng(lat, lon);
				wGoogleMap.getAddress(point,tvAddress);
				return popParkView;
			}
			
			@Override
			public View getInfoContents(Marker arg0) {
				// TODO Auto-generated method stub
				return null;
			}
		});	
	}
	
	/**
	 * 在地图上显示停留图标
	 */
	public void showParkOnMap(String gpsTime, String duration, String Lat,String Lon) {
		double lat = Float.valueOf(Lat);
		double lon = Float.valueOf(Lon);
		LatLng point = new LatLng(lat, lon);
		Bundle bundle = new Bundle();
		bundle.putString("gpsTime", gpsTime);
		bundle.putString("duration", duration);
		bundle.putString("Lat", Lat);
		bundle.putString("Lon", Lon);
		// 构建Marker图标
		BitmapDescriptor bitmap = BitmapDescriptorFactory.fromBitmap(bitmapParking);
		MarkerOptions options = wGoogleMap.setMarkOptions(point, bitmap);
		Marker marker = (Marker) (wGoogleMap.addMarker(options));
		marker.setTitle(Park_Marker_Title);
		parkMakerList.add(marker);
		bundleList.add(bundle);
	}

	/**
	 * 
	 * 画停车标示
	 * 
	 */
	public void addParkOverlay(CarInfo lastCarInfo, CarInfo thisCarInfo) {
		if (lastCarInfo == null || thisCarInfo == null) {
			return;
		}

		// 判断上一步是否是运行状态
		boolean runStatus = ResolveData.hasStatusRun(lastCarPath.getUniStatus());
		if (runStatus) {
			// 上一步运行，返回
			return;
		}

		// 上一步熄火,判断熄火分钟数
		long duration = GetSystem.getStopDuration(lastCarInfo.getGps_time(),
				thisCarInfo.getGps_time());

		if (duration < 5) {
			// 小于五分钟不算
			return;
		}
		String gpsTime = lastCarInfo.getGps_time();
		String minutes = GetSystem.duration2String(duration * 60);
		String Lat = lastCarInfo.getLat();
		String Lon = lastCarInfo.getLon();
		showParkOnMap(gpsTime, minutes, Lat, Lon);
	}
	
	@Override
	public void time(String startTime, String endTime) {
		// TODO Auto-generated method stub
		timeDialog.closeDialog();
		this.lastCarPath = null;
		trackView.load(carInfoList.get(currentCarIndex), startTime, endTime);
	}
	
}

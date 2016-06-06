package com.wisegps.clzx.view;

import java.util.ArrayList;
import java.util.List;

import android.OnFailure;
import android.OnSuccess;
import android.WGeocoder;
import android.WGoogleMap;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.MapView;
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
import com.wisegps.clzx.util.TimeFactory;
import com.wisegps.clzx.view.adapter.OnCarAutoSelectedListener;
import com.wisegps.clzx.view.adapter.OnCarSelectedByKeyListener;

public class GoogleMapViewHelper {

	
	private Context context;
	private float zoom = 16;
	private GoogleMap googleMap;
	private WGoogleMap wGoogleMap;
	
	private View popParkView;// 停车标识弹出信息
	private View popCarView;// 停车标识弹出信息

	private List<Marker> carMakerList = new ArrayList<Marker>();// 车辆标记
	private List<Marker> parkMakerList = new ArrayList<Marker>();// 停车标记
	
//	private List<Overlay> pathOverlayList = new ArrayList<Overlay>();// 轨迹线标记
	
	private List<CarInfo> carInfoList = new ArrayList<CarInfo>();// 车辆信息

	private CarInfo lastCarPath;// 上一次轨迹点
	private TimeDialog timeDialog = null;// 轨迹回放时间选择框
	private int currentCarIndex = 0; // 当前显示车辆在车辆信息列表总的位置

	// bitmap 信息，不用时及时 recycle
	private ArrayList<BitmapDescriptor> btDescList = new ArrayList<BitmapDescriptor>();
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
		
		this.context = context;
		this.googleMap = googleMap;
		wGoogleMap = new WGoogleMap(context,googleMap);
		
		this.googleMap.setOnMarkerClickListener(onMarkerClickListener);

//		this.googleMap.setOnMapStatusChangeListener(onMapStatusChangeListener);
		
		
		this.mapBiz = new MapBiz(handler);
		this.trackView = new TrackView(context, handler);
		this.flowView = new FlowView(context, handler);
		// 实现OnClickTimeDialogListener接口，在time函数返回时间
//		this.timeDialog = new TimeDialog(context, this);
		tvAddress = (TextView) ((Activity) context).findViewById(R.id.tv_address);
		bitmapParking = BitmapFactory.decodeResource(context.getResources(),
				R.drawable.park);
		
		OnCarAutoSelectedListener onCarAutoSelectedListener = new OnCarAutoSelectedListener(){

			@Override
			public void onCarAutoSelected(CarInfo carInfo) {
				onCarSelectedByKeyListener.onCarSelected(carInfo);
//				carinfos.add(carInfo);
//				//消除objId重复项目
//				Map map = list2Map(carinfos); 
//				carinfos = map2List(map);
//				clickCarItem(carinfos.size()-1);
			}
			
		};
		
		
		//车辆搜索框
		carInfoAutoText = new CarInfoAutoText(context,R.id.et_MapSearch,onCarAutoSelectedListener);
		
	}
	
	
	
	private Handler handler = new Handler() {
		@Override
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
				flowView.start(carInfoList.get(currentCarIndex));
				break;
//			case Msg.Track_Draw_Line:
//				Log.i("MapViewHelper", "Track_Draw_Line"+TimeFactory.getCurrentStringTime());
//				Bundle bundle = msg.getData();
//				CarInfo carPath = (CarInfo) bundle.getSerializable("CarPath");
//				drawLine(carPath);
//				
//				break;
			case Msg.Track_Stop:
				Log.i("MapViewHelper", "Track_Stop"+TimeFactory.getCurrentStringTime());
				int index = currentCarIndex;
				reset();
				currentCarIndex = index;
				showAllCar(carInfoList, currentCarIndex);
				break;
			case Msg.Track_Is_Null:
				break;
//			case Msg.Track_Not_Null:
//				//如果再本界面需要清除车辆图标
//				clearCarMarkerList();
//				//如果再播放界面，重新播放时，需要删除上次播放轨迹
//				clearPathList();
//				mBaiduMap.hideInfoWindow();
//				lastCarPath = null;
//				break;
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
//				String tel = (String) msg.obj;
//				try {// 平板没有电话模块异常
//					Intent callIntent = new Intent(Intent.ACTION_DIAL,
//							Uri.parse("tel:" + tel ));
//					context.startActivity(callIntent);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//				hideCarInfoWindow();
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
		ContacterData contacterData = ((MainActivity) context).getCurrentUserInfo();
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
		Bitmap carBitmap = AllStaticClass.getCarBimpMap(context, CarStatus,
				Direct);
		String Lat = carInfo.getLat();
		String Lon = carInfo.getLon();
		double lat = Float.valueOf(Lat);
		double lon = Float.valueOf(Lon);
		LatLng point = new LatLng(lat, lon);
	
		// 构建Marker图标
		BitmapDescriptor bitmap = BitmapDescriptorFactory.fromBitmap(carBitmap);
		btDescList.add(bitmap);
//		Bundle bundle = new Bundle();
//		bundle.putInt("index", index);
		
		MarkerOptions options = wGoogleMap.setMarkOptions(point, bitmap);
		Marker marker = (Marker) (wGoogleMap.addMarker(options));
		marker.setTitle(Car_Marker_Title);
		carMakerList.add(marker);
	}
	
	public void setFocusCar(int index) {
		if((carInfoList.size()>0)){
			String Lat = this.carInfoList.get(index).getLat();
			String Lon = this.carInfoList.get(index).getLon();
			double lat = Float.valueOf(Lat);
			double lon = Float.valueOf(Lon);
			LatLng point = new LatLng(lat, lon);
			wGoogleMap.animateCamera(point, zoom, 0, 0);
			wGoogleMap.getAddress(point,tvAddress);
			showMarkWindow(index);
		}
		
		Log.d("CLZX_GOOGLE", "----" + carInfoList.size() + "---" + index);
		
	}
	
	
	
	
	String managerPhone;//manager电话
	String drivePhone;//drive电话
	public void showMarkWindow(final int index){
		
		currentCarIndex = index;
		final CarInfo carInfo = carInfoList.get(index);
		googleMap.setInfoWindowAdapter(new InfoWindowAdapter() {
			
			@Override
			public View getInfoWindow(Marker arg0) {
				// TODO Auto-generated method stub
				popCarView = mapBiz.getPopView(context, carInfo);
				TextView m_phone = (TextView)popCarView.findViewById(R.id.tv_manager_phone);
				TextView d_phone = (TextView)popCarView.findViewById(R.id.tv_driver_phone);
				drivePhone = d_phone.getText().toString();
				managerPhone = m_phone.getText().toString();
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
				// TODO Auto-generated method stub
				
				marker.hideInfoWindow();
				Toast.makeText(context, "ddd", Toast.LENGTH_SHORT).show();
				
				
			}
		});
	
		
		
	}
	
//	/**
//	 * 显示车辆信息
//	 * 
//	 * @param index
//	 */
//	public void showPopup(int index) {
//		currentCarIndex = index;
//		CarInfo carInfo = carInfoList.get(index);
//		
//		popCarView = mapBiz.getPopView(context, carInfo);
//		
//		popCarView.setOnTouchListener(new OnTouchListener() {
//			@Override
//			public boolean onTouch(View arg0, MotionEvent arg1) {
//				mBaiduMap.hideInfoWindow();
//				return false;
//			}
//		});
//		// 定义用于显示该InfoWindow的坐标点
//		String Lat = carInfoList.get(index).getLat();
//		String Lon = carInfoList.get(index).getLon();
//		double lat = Float.valueOf(Lat);
//		double lon = Float.valueOf(Lon);
//		LatLng point = new LatLng(lat, lon);
//		geoCoder.reverseGeoCode(new ReverseGeoCodeOption().location(point));
//		// 创建InfoWindow , 传入 view， 地理坐标， y 轴偏移量
//		InfoWindow carInfoWindow = new InfoWindow(popCarView, point, 0);
//		// 显示InfoWindow
//		mBaiduMap.showInfoWindow(carInfoWindow);
//	}
	
	public void reset() {
		trackView.reset();
		flowView.reset();
		
//		mBaiduMap.hideInfoWindow();
//		clearCarMarkerList();
//		clearPathList();
		
		
		//mBaiduMap.clear();
		currentCarIndex = 0;
		zoom = 16;
		this.lastCarPath = null;
		tvAddress.setText("");
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
						zoom = 18;
						wGoogleMap.animateCamera(marker.getPosition(), zoom, 0, 0);
						showMarkWindow(i);
					}
				}
			} else if (title.equals(Park_Marker_Title)) {
//				showPopParking(marker.getExtraInfo());
			}

			wGoogleMap.getAddress(marker.getPosition(),tvAddress);
			return false;
		}
	};
	
	public void setOnCarSelectByKeyListener(OnCarSelectedByKeyListener onCarSelectedByKeyListener){
		this.onCarSelectedByKeyListener = onCarSelectedByKeyListener;
	}
	
}

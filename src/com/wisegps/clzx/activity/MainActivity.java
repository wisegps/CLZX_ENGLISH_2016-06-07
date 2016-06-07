package com.wisegps.clzx.activity;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.wisegps.clzx.R;
import com.wisegps.clzx.app.IntentExtra;
import com.wisegps.clzx.biz.Jpush;
import com.wisegps.clzx.entity.CarInfo;
import com.wisegps.clzx.entity.ContacterData;
import com.wisegps.clzx.view.CarViewHelper;
import com.wisegps.clzx.view.ChangepsdView;
import com.wisegps.clzx.view.ConfigView;
import com.wisegps.clzx.view.GoogleMapViewHelper;
//import com.wisegps.clzx.view.MapViewHelper;
import com.wisegps.clzx.view.adapter.OnCarSelectedByKeyListener;
import com.wisegps.clzx.view.adapter.OnCarSelectedListener;

import android.WGoogleMap;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

public class MainActivity extends Activity implements OnClickListener,
		OnCarSelectedListener, OnCarSelectedByKeyListener, OnMapReadyCallback {
	
	private CarViewHelper carViewHelper;// 用户车辆页面
	private GoogleMapViewHelper googleMapViewHelper;
	private ViewFlipper flipper;
	private Jpush jpush = null;
	private GoogleMap googleMap;
	private WGoogleMap wgoogleMap;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		flipper = (ViewFlipper) this.findViewById(R.id.viewFlipper);
		LayoutInflater mLayoutInflater = LayoutInflater.from(this);
		// 车辆列表页面
		View searchView = mLayoutInflater.inflate(R.layout.search, null);
		flipper.addView(searchView);
		// 地图页面
		View mapView = mLayoutInflater.inflate(R.layout.map, null);
		flipper.addView(mapView);
		
		MapFragment mapFragment = (MapFragment) getFragmentManager()
        	    .findFragmentById(R.id.MapView);
        mapFragment.getMapAsync(this); 
        googleMap = mapFragment.getMap();
        wgoogleMap = new WGoogleMap(this,googleMap);
       
		carViewHelper = new CarViewHelper(this);
		// 实现点击车辆列表,切换车辆监听
		carViewHelper.setOnCarSelectedListener(this);
		
		googleMapViewHelper = new GoogleMapViewHelper(this, googleMap);
		googleMapViewHelper.setOnCarSelectByKeyListener(this);
		findViewById(R.id.tv_set_map_type).setOnClickListener(this);
		jpush = new Jpush(this);
		jpush.initJpushSdk();
		showLetterActivity();
		initDrawer();

	}

	public void initDrawer() {
		final DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer);
		ListView listView = (ListView) findViewById(R.id.left_drawer);
		listView.setAdapter(new BaseAdapter(){
			 String[] strs = {"我的信息" ,"修改密码", "参数设置" };
			 int[] icons = {android.R.drawable.sym_action_chat,android.R.drawable.ic_lock_lock,android.R.drawable.ic_menu_manage};
			@Override
			public int getCount() {
				return strs.length;
			}

			@Override
			public Object getItem(int arg0) {
				return null;
			}

			@Override
			public long getItemId(int arg0) {
				return 0;
			}

			@Override
			public View getView(int index, View view, ViewGroup arg2) {
				
				if(view == null){
					LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
					view = inflater.inflate(R.layout.item_list_drawer, null);
				}
				((TextView)view).setText(strs[index]);
				 Drawable drawable= MainActivity.this.getResources().getDrawable(icons[index]);  
			     drawable.setBounds( 0 ,  0 , drawable.getMinimumWidth(), drawable.getMinimumHeight()); 
			     ((TextView)view).setCompoundDrawablePadding(10);
				((TextView)view).setCompoundDrawables(drawable, null, null, null);
				return view;
			}
			
		});
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int index,
					long arg3) {
				drawerLayout.closeDrawers();
				set(index);
			}
		});
		// 侧滑菜单
	}

	public void showLetterActivity() {
		Intent intent = getIntent();
		boolean show = intent.getBooleanExtra(IntentExtra.ShowLetter, false);
		if (show) {
			Intent iLetter = new Intent(MainActivity.this, LetterActivity.class);
			startActivity(iLetter);
		}

	}

	public ContacterData getCurrentUserInfo() {
		return carViewHelper.getCurrentUserInfo();
	}

	/**
	 * 点击车辆列表，选择车辆
	 */
	@Override
	public void onCarSelected(final int index) {
		Log.i("MainActivity", "onCarSelected" + index);
		if (googleMapViewHelper.isRunning()) {
			googleMapViewHelper.stopFlow();
			googleMapViewHelper.stopTrack();
		}

		new Handler().postDelayed(new Runnable() {

			@Override
			public void run() {
				
				googleMapViewHelper.showAllCar(carViewHelper.getCarinfos(), index);
				flipper.setDisplayedChild(1);
			}

		}, 100);

	}

	@Override
	public void onCarSelected(CarInfo carInfo) {
		carViewHelper.onCarSelectByKey(carInfo);
	}

	/**
	 * 点击车辆列表，选择车辆,不跳转
	 */
	@Override
	public void onCarSelectedDefault(int index) {
		Log.i("MainActivity", "onCarSelectedDefault" + index);
		// 如果没有跟踪，没有轨迹回放，就重新刷新绘制车辆
		if (googleMapViewHelper.isRunning() == false) {
			googleMapViewHelper.showAllCar(carViewHelper.getCarinfos(), index);
		}

	}

	/**
	 * 通知更新
	 */
	@Override
	public void onCarRefresh() {
		// 如果没有跟踪，没有轨迹回放，就重新刷新绘制车辆
		if (googleMapViewHelper.isRunning() == false) {
			googleMapViewHelper.refreshCar(carViewHelper.getCarinfos());
		}
	}

	@Override
	public void onClick(View view) {

		switch (view.getId()) {

		case R.id.iv_Map:// 跳转到地图页
			flipper.setDisplayedChild(1);
			break;

		case R.id.iv_Search:// 跳转到列表页
			flipper.setDisplayedChild(0);
			googleMapViewHelper.stopFlow();// 停止跟踪轨迹
			break;
		case R.id.iv_map_traffic_set:// 交通图显示
			setTraffic(view);

			break;
		case R.id.tv_set_map_type:// 卫星图显示
			setMapType(view);
			break;
		}

	}

	/**
	 * @param view
	 */
	public void setTraffic(View view) {
		String isTraffic = (String) view.getTag();
		if (isTraffic.equals("default")) {
			((ImageView) view).setImageResource(R.drawable.main_icon_roadcondition_on);
			wgoogleMap.setTrafficEnabled(true);
			view.setTag("");
			Toast.makeText(this, "实时路况已打开", Toast.LENGTH_SHORT).show();
		} else {
			((ImageView) view).setImageResource(R.drawable.main_icon_roadcondition_off);
			wgoogleMap.setTrafficEnabled(false);
			view.setTag("default");
			Toast.makeText(this, "实时路况已关闭", Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * @param view 
	 */
	public void setMapType(View view) {
		String isSatellite = (String) view.getTag();
		if (isSatellite.equals("default")) {// 设置为卫星模式
			wgoogleMap.setMapType(WGoogleMap.MAP_TYPE_SATELLITE);
			((TextView) view).setText(R.string.Satellite);
			view.setTag("");
		} else {// 设置为普通地图
			wgoogleMap.setMapType(WGoogleMap.MAP_TYPE_NORMAL);
			((TextView) view).setText(R.string.Traffic);
			view.setTag("default");
		}
	}

	@Override
	public void finish() {
		super.finish();
	}

	@Override
	protected void onDestroy() {
		carViewHelper.stopReresh();
		super.onDestroy();
//		mMapView.onDestroy();
		// jpush.stopPush();
	}

	@Override
	protected void onResume() {
		super.onResume();
//		mMapView.onResume();
		jpush.resume();
	}

	@Override
	protected void onPause() {
		super.onPause();
//		mMapView.onPause();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			carViewHelper.stopReresh();
			googleMapViewHelper.reset();
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		// menu.add(0, 1, 0, "参数设置");
		// menu.add(0, 2, 0, "修改密码");
		// menu.add(0, 3, 0, "我的消息");

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		set(item.getItemId());
		return super.onOptionsItemSelected(item);
	}

	public void set(int id) {
		switch (id) {
		case 0:
			Intent iLetter = new Intent(MainActivity.this, LetterActivity.class);
			startActivity(iLetter);
			break;
		case 1:
			ChangepsdView changepsd = new ChangepsdView(this);
			changepsd.changePwd();
			break;
		case 2:
			ConfigView configView = new ConfigView(this);
			configView.setting();
			break;
		}
	}

	@Override
	public void onMapReady(GoogleMap map) {
		// TODO Auto-generated method stub
		map.getUiSettings().setZoomControlsEnabled(true);//在视图上面显示缩放比例控件
		map.getUiSettings().setMapToolbarEnabled(false);//地图工具栏
		map.setMapType(WGoogleMap.MAP_TYPE_NORMAL);//普通街道地图
	}

}

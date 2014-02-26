package com.damon.bikelife;


import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import org.apache.http.util.EncodingUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.ab.activity.AbActivity;
import com.ab.view.titlebar.AbTitleBar;
import com.ant.liao.GifView;
import com.ant.liao.GifView.GifImageType;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.BMapManager;
import com.baidu.mapapi.map.ItemizedOverlay;
import com.baidu.mapapi.map.LocationData;
import com.baidu.mapapi.map.MapController;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationOverlay;
import com.baidu.mapapi.map.MyLocationOverlay.LocationMode;
import com.baidu.mapapi.map.OverlayItem;
import com.baidu.mapapi.map.PopupClickListener;
import com.baidu.mapapi.map.PopupOverlay;
import com.baidu.mobads.IconsAd;
import com.baidu.mobads.appoffers.OffersManager;
import com.baidu.platform.comapi.basestruct.GeoPoint;
import com.damon.bikelife.city.CityListActivity;
import com.damon.bikelife.utils.WidgetController;



public class MapActivity extends AbActivity {

    
	private enum E_BUTTON_TYPE {
		LOC,
		COMPASS,
		FOLLOW
	}
	
	private E_BUTTON_TYPE mCurBtnType;

	// 定位相关
	LocationClient mLocClient;
	LocationData locData = null;
	public MyLocationListenner myListener = new MyLocationListenner();
	
	//定位图层
	locationOverlay myLocationOverlay = null;
	//弹出泡泡图层
	private PopupOverlay   pop  = null;//弹出泡泡图层，浏览节点时使用
	private TextView  popupMyLocation = null;
	private TextView  popupText = null;//泡泡view
	private TextView popaddress = null;
	private GifView popBnum = null;
	private GifView popPnum = null;
	private TextView popBnum1 = null;
	private TextView popPnum1 = null;
	LinearLayout layout = null;
	private View viewCache = null;
	private ProgressBar progressBar = null;
	
	//地图相关，使用继承MapView的MyLocationMapView目的是重写touch事件实现泡泡处理
	//如果不处理touch事件，则无需继承，直接使用MapView即可
	MyLocationMapView mMapView = null;	// 地图View
	private MapController mMapController = null;

	//UI相关
	Button requestLocButton = null;
	boolean isRequest = false;//是否手动触发请求定位
	boolean isFirstLoc = true;//是否首次定位
	
	//标题栏
    private AbTitleBar mAbTitleBar = null;
    //private List<String> cityList;
    
    private MyOverlay mOverlay = null;
    private String curCity = null;
	private ArrayList<OverlayItem>  mItems = null; 
    private OverlayItem curItem =null;
    
    private Dialog dialog = null;
    private SelectDialog selectDialog = null;
    
    private boolean isQuit = false;
    private Handler handler = null;
    
    private String path = null;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /**
         * 使用地图sdk前需先初始化BMapManager.
         * BMapManager是全局的，可为多个MapView共用，它需要地图模块创建前创建，
         * 并在地图地图模块销毁后销毁，只要还有地图模块在使用，BMapManager就不应该销毁
         */
        MainApplication app = (MainApplication)this.getApplication();
        if (app.mBMapManager == null) {
            app.mBMapManager = new BMapManager(getApplicationContext());
            /**
             * 如果BMapManager没有初始化则初始化BMapManager
             */
            app.mBMapManager.init(MainApplication.strKey,new MainApplication.MyGeneralListener());
        }
        setAbContentView(R.layout.map_main);
        
        mAbTitleBar = this.getTitleBar();
        mAbTitleBar.setTitleText("城市选择");
        mAbTitleBar.setLogo(R.drawable.button_selector_back);
        mAbTitleBar.setTitleBarBackground(R.drawable.top_bg);
        mAbTitleBar.setTitleTextMargin(100, 0, 0, 0);
        mAbTitleBar.setLogoLine(R.drawable.line);
        mAbTitleBar.setVisibility(View.VISIBLE);
        mAbTitleBar.setTitleTextBackgroundResource(R.drawable.drop_down_title_btn);
        mAbTitleBar.setTitleTextOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent intent = new Intent(MapActivity.this,CityListActivity.class);
 				startActivityForResult(intent, 0);
			}
		});
        mAbTitleBar.setLogoOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(isQuit){
					finish();
				}else {
					Toast.makeText(MapActivity.this, "再按一次退出！", Toast.LENGTH_SHORT).show();
					isQuit = true;
					handler.postDelayed(new Runnable() {
						
						@Override
						public void run() {
							// TODO Auto-generated method stub
							isQuit = false;
						}
					}, 2000);
				}		
			}
		});
        	
        View rightViewMore = mInflater.inflate(R.layout.more_btn, null);
    	mAbTitleBar.addRightView(rightViewMore);
    	Button about = (Button)rightViewMore.findViewById(R.id.moreBtn);
    	
    	selectDialog = new SelectDialog(MapActivity.this,R.style.dialog);//创建Dialog并设置样式主题
		Window dialogWindow = selectDialog.getWindow();
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        dialogWindow.setGravity(Gravity.RIGHT | Gravity.TOP);    
        lp.y = WidgetController.getHeight(mAbTitleBar);
        
        dialogWindow.setAttributes(lp);
		selectDialog.setCanceledOnTouchOutside(true);//设置点击Dialog外部任意区域关闭Dialog
    	
    	about.setOnClickListener(new View.OnClickListener(){

 			@Override
 			public void onClick(View v) {
 				if(selectDialog.isShowing()){
 					selectDialog.dismiss();
 				}else {
 					selectDialog.show();
				}
 			}
         	
         });
		progressBar = (ProgressBar)findViewById(R.id.progressBar1);
        requestLocButton = (Button)findViewById(R.id.button1);
        mCurBtnType = E_BUTTON_TYPE.LOC;
        OnClickListener btnClickListener = new OnClickListener() {
        	public void onClick(View v) {
				switch (mCurBtnType) {
				case LOC:
					//手动定位请求
					requestLocClick();
					break;
				case COMPASS:
					myLocationOverlay.setLocationMode(LocationMode.NORMAL);
					requestLocButton.setText("定位");
					mCurBtnType = E_BUTTON_TYPE.LOC;
					break;
				case FOLLOW:
					myLocationOverlay.setLocationMode(LocationMode.COMPASS);
					requestLocButton.setText("罗盘");
					mCurBtnType = E_BUTTON_TYPE.COMPASS;
					break;
				}
			}
		};
	    requestLocButton.setOnClickListener(btnClickListener);
	    
        handler = new Handler();
		//地图初始化
        mMapView = (MyLocationMapView)findViewById(R.id.bmapView);
        mMapController = mMapView.getController();
        mMapView.getController().setZoom(14);
        mMapView.getController().enableClick(true);
        mMapView.setBuiltInZoomControls(true);
        mMapView.setOnTouchListener(new View.OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				myLocationOverlay.setLocationMode(LocationMode.NORMAL);
				requestLocButton.setText("定位");
				return false;
			}
		});
      //创建 弹出泡泡图层
        createPaopao();
        
        //定位初始化
        mLocClient = new LocationClient( this );
        locData = new LocationData();
        mLocClient.registerLocationListener( myListener );
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true);//打开gps
        option.setCoorType("bd09ll");     //设置坐标类型
        option.setAddrType("all");
        option.setScanSpan(1000);
        mLocClient.setLocOption(option);
        mLocClient.start();
       
        //定位图层初始化
		myLocationOverlay = new locationOverlay(mMapView);
		//设置定位数据
	    myLocationOverlay.setData(locData);
	    //添加定位图层
		mMapView.getOverlays().add(myLocationOverlay);
		myLocationOverlay.enableCompass();
		//修改定位数据后刷新图层生效
		mMapView.refresh();

		initData();		//初始化数据
		
		IconsAd iconsAd=new IconsAd(this);
		iconsAd.loadAd(this);
		
		IntentFilter filter = new IntentFilter(); 
        filter.addAction("com.damon.action.openadwall"); 
        filter.setPriority(Integer.MAX_VALUE); 
        registerReceiver(myReceiver, filter);
		
    }
    /**
     * 手动触发一次定位请求
     */
    public void requestLocClick(){
    	isRequest = true;
        mLocClient.requestLocation();
        Toast.makeText(this, "正在定位……", Toast.LENGTH_SHORT).show();
    }
    /**
     * 修改位置图标
     * @param marker
     */
    public void modifyLocationOverlayIcon(Drawable marker){
    	//当传入marker为null时，使用默认图标绘制
    	myLocationOverlay.setMarker(marker);
    	//修改图层，需要刷新MapView生效
    	mMapView.refresh();
    }
    /**
	 * 创建弹出泡泡图层
	 */
	public void createPaopao(){
		viewCache = getLayoutInflater().inflate(R.layout.custom_text_view, null);
		popupMyLocation = (TextView) viewCache.findViewById(R.id.mylocation);
        popupText =(TextView) viewCache.findViewById(R.id.textcache);
        popaddress = (TextView) viewCache.findViewById(R.id.address);
        popBnum = (GifView) viewCache.findViewById(R.id.bikenum);
        popPnum = (GifView) viewCache.findViewById(R.id.parkingnum);
        popBnum.setGifImageType(GifImageType.COVER);
        popPnum.setGifImageType(GifImageType.COVER);
        popBnum1 = (TextView) viewCache.findViewById(R.id.bikenum1);
        popPnum1 = (TextView) viewCache.findViewById(R.id.parkingnum1);
        layout = (LinearLayout) viewCache.findViewById(R.id.layout);
        //泡泡点击响应回调
        PopupClickListener popListener = new PopupClickListener(){
			@Override
			public void onClickedPopup(int index) {
				Log.v("click", "clickapoapo");
			}
        };
        pop = new PopupOverlay(mMapView,popListener);
        MyLocationMapView.pop = pop;
	}
	/**
     * 定位SDK监听函数
     */
    public class MyLocationListenner implements BDLocationListener {
    	
        @Override
        public void onReceiveLocation(BDLocation location) {
            if (location == null)
                return ;
            
            locData.latitude = location.getLatitude();
            locData.longitude = location.getLongitude();
            //如果不显示定位精度圈，将accuracy赋值为0即可
            locData.accuracy = location.getRadius();
            // 此处可以设置 locData的方向信息, 如果定位 SDK 未返回方向信息，用户可以自己实现罗盘功能添加方向信息。
            locData.direction = location.getDerect();
            //更新定位数据
            myLocationOverlay.setData(locData);
            //更新图层数据执行刷新后生效
            mMapView.refresh();
            //是手动触发请求或首次定位时，移动到定位点
            if (isRequest || isFirstLoc){
            	//移动地图到定位点
                mMapController.animateTo(new GeoPoint((int)(locData.latitude* 1e6), (int)(locData.longitude *  1e6)));
                isRequest = false;
                myLocationOverlay.setLocationMode(LocationMode.FOLLOWING);
				requestLocButton.setText("跟随");
                mCurBtnType = E_BUTTON_TYPE.FOLLOW;
                String city = location.getCity();
                city = city.replace("市", "");
                mAbTitleBar.setTitleText(city);
                BikeInfo bikeInfo = getBikeInfo(city);
                if(bikeInfo != null){
                	curCity = bikeInfo.getCityName();
					mAbTitleBar.hideWindow();
					animateToCity(curCity);
                }    
            }
            //首次定位完成
            isFirstLoc = false;
        }
        
        public void onReceivePoi(BDLocation poiLocation) {
            if (poiLocation == null){
                return ;
            }
        }
    }
    
    //继承MyLocationOverlay重写dispatchTap实现点击处理
  	public class locationOverlay extends MyLocationOverlay{

  		public locationOverlay(MapView mapView) {
  			super(mapView);
  			// TODO Auto-generated constructor stub
  		}
  		@Override
  		protected boolean dispatchTap() {
  			// TODO Auto-generated method stub
  			//处理点击事件,弹出泡泡
  			popupMyLocation.setBackgroundResource(R.drawable.popup);
  			popupMyLocation.setText("我的位置");
			pop.showPopup(BMapUtil.getBitmapFromView(popupMyLocation),
					new GeoPoint((int)(locData.latitude*1e6), (int)(locData.longitude*1e6)),
					8);
  			return true;
  		}
  		
  	}

    @Override
    protected void onPause() {
        mMapView.onPause();
        super.onPause();
    }
    
    @Override
    protected void onResume() {
        mMapView.onResume();
        super.onResume();
    }
    
    public boolean onKeyDown(int keyCode, KeyEvent event){
		if(keyCode == KeyEvent.KEYCODE_BACK){
			if(isQuit){
				return super.onKeyDown(keyCode, event);
			}else {
				Toast.makeText(MapActivity.this, "再按一次退出！", Toast.LENGTH_SHORT).show();
				isQuit = true;
				handler.postDelayed(new Runnable() {
					
					@Override
					public void run() {
						// TODO Auto-generated method stub
						isQuit = false;
					}
				}, 2000);
				return false;
			}
			
		}else {
			return super.onKeyDown(keyCode, event);
		}
    }
    
    @Override
    protected void onDestroy() {
    	//退出时销毁定位
    	unregisterReceiver(myReceiver);
        if (mLocClient != null)
            mLocClient.stop();
        mMapView.destroy();
        super.onDestroy();
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	mMapView.onSaveInstanceState(outState);
    	
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
    	super.onRestoreInstanceState(savedInstanceState);
    	mMapView.onRestoreInstanceState(savedInstanceState);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    
    private void initData(){
    	path = this.getApplicationContext().getFilesDir().getPath()+"/BikeLife.db";
    	PackageInfo info = null;
		try {
			info = getPackageManager().getPackageInfo("com.damon.bikelife", 0);
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
    	int currentVersion = info.versionCode;  
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);  
    	int lastVersion = prefs.getInt("VERSION_KEY", 0);  
    	if (currentVersion > lastVersion) {  
    		copyAssetsToFilesystem("BikeLife.db", path);
    	    prefs.edit().putInt("VERSION_KEY",currentVersion).commit();  
    	}  
    }
    
    private boolean copyAssetsToFilesystem(String assetsSrc, String des){  
 
        InputStream istream = null;  
        OutputStream ostream = null;  
        try{  
            AssetManager am = this.getAssets();  
            istream = am.open(assetsSrc);  
            ostream = new FileOutputStream(des);  
            byte[] buffer = new byte[1024];  
            int length;  
            while ((length = istream.read(buffer))>0){  
                ostream.write(buffer, 0, length);  
            }  
            istream.close();  
            ostream.close();  
        }  
        catch(Exception e){  
            e.printStackTrace();  
            try{  
                if(istream!=null)  
                    istream.close();  
                if(ostream!=null)  
                    ostream.close();  
            }  
            catch(Exception ee){  
                ee.printStackTrace();  
            }  
            return false;  
        }  
        return true;  
    }  
	  
    private void animateToCity(String cityName){
    	BikeInfo bikeInfo = getBikeInfo(cityName); 
    	
    	if(bikeInfo == null){
			Toast.makeText(this, "信息未找到！", Toast.LENGTH_SHORT).show();
		}
    	
    	//BikeInfo bikeInfo = map.get(cityName);
    	LocationData cenData = bikeInfo.getLocData();
    	myLocationOverlay.setLocationMode(LocationMode.NORMAL);
    	requestLocButton.setText("定位");
    	if(mOverlay != null){
    		mMapView.getOverlays().remove(mOverlay);
    		mMapView.refresh();
    	}
    	
    	mMapController.setCenter(new GeoPoint((int)(cenData.latitude* 1e6), (int)(cenData.longitude *  1e6)));
    	String sJson = getFromAssets(bikeInfo.getFileName());
    	/**
    	 * 创建自定义overlay
    	 */
         mOverlay = new MyOverlay(getResources().getDrawable(R.drawable.marker),mMapView);	

		try {
			JSONArray locateArray = new JSONArray(sJson);
			int iSize = locateArray.length();
	    	for (int i = 0; i < iSize; i++) {
	    	JSONObject jsonObj = locateArray.getJSONObject(i);
	    	GeoPoint p = null;
	    	if(bikeInfo.getGeoType().equals("bd09ll")){
	    		p = getFrombd09ll(jsonObj.getDouble("lat"), jsonObj.getDouble("lng"));
	    	}else if(bikeInfo.getGeoType().equals("gcj02")){
	    		p = gcEncTobd(jsonObj.getDouble("lat"), jsonObj.getDouble("lng"));
			}
	    	
	        OverlayItem item = new OverlayItem(p,jsonObj.getString("id")+":"+jsonObj.getString("name"),jsonObj.getString("address"));
	        mOverlay.addItem(item);
	    	}
	    	mMapView.getOverlays().add(mOverlay);
	    	mMapView.refresh(); 
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	
    }
    
    private BikeInfo getBikeInfo(String cityName){
    	BikeInfo bikeInfo = null;
    	SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(path, null);
    	Cursor cursor = db.rawQuery("select * from tbl_Citys where cityName =?", new String[]{cityName});
    	if(cursor.moveToFirst()){
    		LocationData cenData = new LocationData();
	    	
	    	cenData.latitude = cursor.getDouble(cursor.getColumnIndex("latitude"));  
	    	cenData.longitude = cursor.getDouble(cursor.getColumnIndex("longitude"));
	    	cenData.direction = 2.0f;
	    	bikeInfo = new BikeInfo(cityName,cursor.getString(cursor.getColumnIndex("fileName")), 
	    			cursor.getString(cursor.getColumnIndex("url")), cenData,
	    			cursor.getString(cursor.getColumnIndex("geoType")));
    	}
    	cursor.close();
    	db.close();
    	return bikeInfo;
    }
    
    /**
     * gcj02经纬坐标转bd09ll经纬坐标得到GeoPoint
     * @param lat
     * @param lon
     * @return GeoPoint
     */
    private GeoPoint gcEncTobd(double lat, double lon){
    	final double x_pi = 3.14159265358979324 * 3000.0 / 180.0;
    	double z = Math.sqrt(lon * lon + lat * lat) + 0.00002 * Math.sin(lon * x_pi); 
    	double theta = Math.atan2(lat, lon) + 0.000003 * Math.cos(lon * x_pi);
    	double mLat = z * Math.sin(theta) + 0.006; 
    	double mLon = z * Math.cos(theta) + 0.0065;
    	GeoPoint p = new GeoPoint ((int)(mLat*1E6),(int)(mLon*1E6));
    	return p;
    }
    
    /**
     * 通过bd09ll经纬坐标得到GeoPoint
     * @param lat
     * @param lon
     * @return GeoPoint
     */
    private GeoPoint getFrombd09ll(double lat, double lon){
    	GeoPoint p = new GeoPoint ((int)(lat*1E6),(int)(lon*1E6));
    	return p;
    }
    
    public String getFromAssets(String fileName){ 
    	String result = "";
    	   try {
    	InputStream in = getResources().getAssets().open(fileName);
    	//获取文件的字节数
    	int lenght = in.available();
    	//创建byte数组
    	byte[]  buffer = new byte[lenght];
    	//将文件中的数据读到byte数组中
    	in.read(buffer);
    	result = EncodingUtils.getString(buffer, "UTF-8");
    	} catch (Exception e) {
    	e.printStackTrace();
    	}
    	return result;
    } 
    
    
    
    
    
    
    public class MyOverlay extends ItemizedOverlay{

		public MyOverlay(Drawable defaultMarker, MapView mapView) {
			super(defaultMarker, mapView);
		}
		

		@Override
		public boolean onTap(int index){
			curItem = getItem(index);
			popupText.setText(curItem.getTitle());
			popaddress.setText("地址："+curItem.getSnippet());
			String id = curItem.getTitle().split(":")[0];
			String url = getBikeInfo(curCity).getUrl();
			if(url.endsWith("?")){
				new  ImageDownLoaderTask2(1).execute(url+"id="+id);
			}else if (url.endsWith("=")) { 
				new ImageDownLoaderTask(1).execute(url+id+"&flag=");	
			}else{
				new  ImageDownLoaderTask2(2).execute(new String[]{url,id});
			}
				
			return true;
		}
		
		@Override
		public boolean onTap(GeoPoint pt , MapView mMapView){
			if (pop != null){
                pop.hidePop();
                
			}
			return false;
		}
    	
    }
    
   
    
    
    class ImageDownLoaderTask extends AsyncTask<String, Void, ArrayList<byte[]>>{
    	private int flag = 1;
    	public ImageDownLoaderTask(int flag) {
			// TODO Auto-generated constructor stub
    		this.flag = flag;
		}
    	
		@Override
		protected ArrayList<byte[]> doInBackground(String... params) {
			// TODO Auto-generated method stub
			ArrayList<byte[]> result = new ArrayList<byte[]>();
			result.add(getImgFromNet(params[0]+"1"));
			result.add(getImgFromNet(params[0]+"2"));
	        return result;  
		}
    	
		
		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			progressBar.setVisibility(View.VISIBLE);
			super.onPreExecute();
		}


		@Override
		protected void onPostExecute(ArrayList<byte[]> result) {
			// TODO Auto-generated method stub	
			progressBar.setVisibility(View.GONE);
			if(result.get(0) != null){
				popBnum.setVisibility(View.VISIBLE);
				popBnum1.setVisibility(View.GONE);
				popBnum.setGifImage(result.get(0));
			}else {
				popBnum.setVisibility(View.GONE);
				popBnum1.setVisibility(View.VISIBLE);
				popBnum1.setText("未知");
			}
			if(result.get(1) != null){
				popPnum.setVisibility(View.VISIBLE);
				popPnum1.setVisibility(View.GONE);
				popPnum.setGifImage(result.get(1));
			}else {
				popPnum.setVisibility(View.GONE);
				popPnum1.setVisibility(View.VISIBLE);
				popPnum1.setText("未知");
			}					
			/*WindowManager.LayoutParams lp = new WindowManager.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
					WindowManager.LayoutParams.TYPE_APPLICATION, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
					PixelFormat.TRANSLUCENT);
			windowManager.addView(viewCache, lp);
			hasView = true;*/
			if(dialog == null){
				dialog = new Dialog(MapActivity.this);
				dialog.setContentView(viewCache);
				dialog.setTitle("详情");
				dialog.setCanceledOnTouchOutside(true);
				Window dialogWindow = dialog.getWindow();
		        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
		        dialogWindow.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
		        lp.alpha = 0.7f; // 透明度
		        dialogWindow.setAttributes(lp);
			}		
	        dialog.show();
			/*Bitmap[] bitMaps={
					    BMapUtil.getBitmapFromView(layout), 		
				    };
			pop.showPopup(bitMaps,curItem.getPoint(),8);*/
			super.onPostExecute(result);
		}		
		
		private byte[] getImgFromNet(String urlString){
			byte[] btImg = null ;
			HttpURLConnection conn = null;
	        try {  
	            URL url = new URL(urlString);  
	            conn = (HttpURLConnection)url.openConnection();  
	            conn.setRequestMethod("GET");  
	            conn.setConnectTimeout(5 * 1000);
	            conn.connect();
	            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
	            	InputStream inStream = conn.getInputStream();//通过输入流获取图片数据        
		            btImg = readInputStream(inStream);//得到图片的二进制数据	 
	            }
	                        
	        }catch (Exception e) {  
	            e.printStackTrace();
	        } finally{
	        	if(conn != null){
	        		conn.disconnect();
	        	}        	
	        }  
	        return btImg; 
		}
		/** 
	     * 从输入流中获取数据 
	     * @param inStream 输入流 
	     * @return 
	     * @throws Exception 
	     */  
	    public  byte[] readInputStream(InputStream inStream) throws Exception{  
	        ByteArrayOutputStream outStream = new ByteArrayOutputStream();  
	        byte[] buffer = new byte[1024];  
	        int len = 0;  
	        while( (len=inStream.read(buffer)) != -1 ){  
	            outStream.write(buffer, 0, len);  
	        }    
	        return outStream.toByteArray();  
	    }
	      
    }
    
    class ImageDownLoaderTask2 extends AsyncTask<String, Void, String[]>{
    	
    	private int flag = 1;
    	 
    	public ImageDownLoaderTask2(int flag) {
			// TODO Auto-generated constructor stub
    		this.flag = flag;
		}

		@Override
		protected String[] doInBackground(String... params) {
			// TODO Auto-generated method stub
			String [] data = null;
			HttpURLConnection conn = null;
	        try {  
	            URL url = new URL(params[0]);  
	            conn = (HttpURLConnection)url.openConnection();  
	            conn.setRequestMethod("GET");  
	            conn.setConnectTimeout(5 * 1000);
	            conn.connect();
	            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
	            	InputStream inStream = conn.getInputStream();
	            	String result = "";
	            	String line = null;
	            	BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
	            	while ((line = reader.readLine()) != null) {
	            		result += line;
	                }
	            	result = result.substring(result.indexOf("["), result.lastIndexOf("]")+1);
	            	JSONArray locateArray = new JSONArray(result);
	            	if(flag == 1){
	            		JSONObject jsonObj = locateArray.getJSONObject(0);
		    	    	data = new String[]{jsonObj.getString("availBike"),(jsonObj.getInt("capacity")-jsonObj.getInt("availBike"))+""};
	            	}else {
	            		int iSize = locateArray.length();
	        	    	for (int i = 0; i < iSize; i++) {
		        	    	JSONObject jsonObj = locateArray.getJSONObject(i);
		        	        if(jsonObj.getString("id").equals(params[1])){
		        	        	data = new String[]{jsonObj.getString("availBike"),(jsonObj.getInt("capacity")-jsonObj.getInt("availBike"))+""};
		        	        	break;
		        	        }
	        	    	}
					}
	    	    	
	            }
	                        
	        }catch (Exception e) {  
	            e.printStackTrace();
	        } finally{
	        	if(conn != null){
	        		conn.disconnect();
	        	}        	
	        }  
			return data;
		}

		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			progressBar.setVisibility(View.VISIBLE);
			super.onPreExecute();
		}

		@Override
		protected void onPostExecute(String[] result) {
			// TODO Auto-generated method stub
			progressBar.setVisibility(View.GONE);
			popBnum.setVisibility(View.GONE);
			popPnum.setVisibility(View.GONE);
			popBnum1.setVisibility(View.VISIBLE);
			popPnum1.setVisibility(View.VISIBLE);
			if(result != null){
				popBnum1.setText(result[0]);
				popPnum1.setText(result[1]);
			}else {
				popBnum1.setText("未知");
				popPnum1.setText("未知");
			}
			/*if(hasView){
				windowManager.removeView(viewCache);
				hasView = false;
			}*/
			if(dialog == null){
				dialog = new Dialog(MapActivity.this);
				dialog.setContentView(viewCache);
				dialog.setTitle("详情");
				dialog.setCanceledOnTouchOutside(true);
				Window dialogWindow = dialog.getWindow();
		        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
		        dialogWindow.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
		        lp.alpha = 0.7f; // 透明度
		        dialogWindow.setAttributes(lp);
			}
			
	        dialog.show();
			/*WindowManager.LayoutParams lp = new WindowManager.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
					WindowManager.LayoutParams.TYPE_APPLICATION, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
					PixelFormat.TRANSLUCENT);
			windowManager.addView(viewCache, lp);
			hasView = true;*/
			/*Bitmap[] bitMaps={
				    BMapUtil.getBitmapFromView(layout), 		
			    };
			pop.showPopup(bitMaps,curItem.getPoint(),8);*/
			super.onPostExecute(result);
		}
    	
		
    }

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		switch(requestCode){
		case 0:
			if(resultCode == Activity.RESULT_OK){				
				curCity = data.getExtras().getString("cityName");
				mAbTitleBar.setTitleText(curCity);
				mAbTitleBar.hideWindow();
				animateToCity(curCity);
			}
			break;
			default:
				break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
    
    
	private BroadcastReceiver myReceiver = new BroadcastReceiver() { 
		 
        @Override 
        public void onReceive(Context context, Intent intent) { 
        	OffersManager.showOffers(context);
        } 
 
    }; 
	
}


/**
 * 继承MapView重写onTouchEvent实现泡泡处理操作
 * @author hejin
 *
 */
class MyLocationMapView extends MapView{
	static PopupOverlay   pop  = null;//弹出泡泡图层，点击图标使用
	public MyLocationMapView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}
	public MyLocationMapView(Context context, AttributeSet attrs){
		super(context,attrs);
	}
	public MyLocationMapView(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
	}
	@Override
    public boolean onTouchEvent(MotionEvent event){
		if (!super.onTouchEvent(event)){
			//消隐泡泡
			if (pop != null && event.getAction() == MotionEvent.ACTION_UP)
				pop.hidePop();
		}
		return true;
	}
}



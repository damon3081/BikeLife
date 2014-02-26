package com.damon.bikelife.city;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.ab.activity.AbActivity;
import com.ab.view.titlebar.AbTitleBar;
import com.damon.bikelife.R;
import com.damon.bikelife.city.LetterView.OnTouchingLetterChangedListener;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * 城市列表
 * 
 * @author damon
 * 
 */
public class CityListActivity extends AbActivity
{
	private BaseAdapter adapter;
	private ListView mCityLit;
	private TextView overlay;
	private LetterView letterListView;
	private HashMap<String, Integer> alphaIndexer;// 存放存在的汉语拼音首字母和与之对应的列表位置
	private Handler handler;
	private OverlayThread overlayThread;
	private ArrayList<CityModel> mCityNames;
	private WindowManager windowManager;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setAbContentView(R.layout.city_list);
		AbTitleBar mAbTitleBar = this.getTitleBar();
		mAbTitleBar.setTitleText("城市切换");
	    mAbTitleBar.setLogo(R.drawable.button_selector_back);
	    mAbTitleBar.setTitleBarBackground(R.drawable.top_bg);
	    mAbTitleBar.setTitleTextMargin(150, 0, 150, 0);	    
	    mAbTitleBar.setLogoLine(R.drawable.line);
	    mAbTitleBar.setVisibility(View.VISIBLE);
	    mAbTitleBar.setLogoOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Intent intent = new Intent();
					setResult(Activity.RESULT_CANCELED, intent);
					finish();
				}
			});
	        
		mCityLit = (ListView) findViewById(R.id.city_list);
		letterListView = (LetterView) findViewById(R.id.cityLetterListView);
		mCityNames = getCityNames();
		letterListView.setOnTouchingLetterChangedListener(new LetterListViewListener());
		alphaIndexer = new HashMap<String, Integer>();
		handler = new Handler();
		overlayThread = new OverlayThread();
		initOverlay();
		setAdapter(mCityNames);
		mCityLit.setOnItemClickListener(new CityListOnItemClick());

	}

	/**
	 * 
	 * 
	 * @return
	 */
	private ArrayList<CityModel> getCityNames()
	{
		ArrayList<CityModel> names = new ArrayList<CityModel>();
		String path = this.getApplicationContext().getFilesDir().getPath()+"/BikeLife.db";
		SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(path, null);
    	Cursor cursor = db.rawQuery("select * from tbl_Citys order by nameSort", null);
    	while(cursor.moveToNext()){
    		
	    	String cityName = cursor.getString(cursor.getColumnIndex("cityName"));
	    	String nameSort = cursor.getString(cursor.getColumnIndex("nameSort")); 
	    	names.add(new CityModel(cityName,nameSort));
	    	
    	}
    	cursor.close();
    	db.close();
		return names;
	}

	/**
	 * 城市列表点击事件
	 * 
	 * @author sy
	 * 
	 */
	class CityListOnItemClick implements OnItemClickListener
	{

		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3)
		{
			CityModel cityModel = (CityModel) mCityLit.getAdapter().getItem(pos);
			Intent intent = new Intent();
			intent.putExtra("cityName", cityModel.getCityName());
			setResult(Activity.RESULT_OK, intent);
			finish();
		}

	}

	/**
	 * 为ListView设置适配器
	 * 
	 * @param list
	 */
	private void setAdapter(List<CityModel> list)
	{
		if (list != null)
		{
			adapter = new ListAdapter(this, list);
			mCityLit.setAdapter(adapter);
		}

	}

	/**
	 * ListViewAdapter
	 * 
	 * @author damon
	 */
	private class ListAdapter extends BaseAdapter
	{
		private LayoutInflater inflater;
		private List<CityModel> list;

		public ListAdapter(Context context, List<CityModel> list)
		{

			this.inflater = LayoutInflater.from(context);
			this.list = list;
			alphaIndexer = new HashMap<String, Integer>();
			for (int i = 0; i < list.size(); i++)
			{
				// 当前汉语拼音首字母
				// getAlpha(list.get(i));
				String currentStr = list.get(i).getNameSort();
				// 上一个汉语拼音首字母，如果不存在为“ ”
				String previewStr = (i - 1) >= 0 ? list.get(i - 1).getNameSort() : " ";
				if (!previewStr.equals(currentStr))
				{
					String name = list.get(i).getNameSort();
					alphaIndexer.put(name, i);
				}
			}

		}

		@Override
		public int getCount()
		{
			return list.size();
		}

		@Override
		public Object getItem(int position)
		{
			return list.get(position);
		}

		@Override
		public long getItemId(int position)
		{
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			ViewHolder holder;
			if (convertView == null)
			{
				convertView = inflater.inflate(R.layout.city_item, null);
				holder = new ViewHolder();
				holder.alpha = (TextView) convertView.findViewById(R.id.alpha);
				holder.name = (TextView) convertView.findViewById(R.id.name);
				convertView.setTag(holder);
			} else
			{
				holder = (ViewHolder) convertView.getTag();
			}

			holder.name.setText(list.get(position).getCityName());
			String currentStr = list.get(position).getNameSort();
			String previewStr = (position - 1) >= 0 ? list.get(position - 1).getNameSort() : " ";
			if (!previewStr.equals(currentStr))
			{
				holder.alpha.setVisibility(View.VISIBLE);
				holder.alpha.setText(currentStr);
			} else
			{
				holder.alpha.setVisibility(View.GONE);
			}
			return convertView;
		}

		private class ViewHolder
		{
			TextView alpha;
			TextView name;
		}

	}

	// 初始化汉语拼音首字母弹出提示框
	private void initOverlay()
	{
		LayoutInflater inflater = LayoutInflater.from(this);
		overlay = (TextView) inflater.inflate(R.layout.overlay, null);
		overlay.setVisibility(View.INVISIBLE);
		WindowManager.LayoutParams lp = new WindowManager.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.TYPE_APPLICATION, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
				PixelFormat.TRANSLUCENT);
		windowManager = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
		windowManager.addView(overlay, lp);
	}

	private class LetterListViewListener implements OnTouchingLetterChangedListener
	{

		@Override
		public void onTouchingLetterChanged(final String s)
		{
			if (alphaIndexer.get(s) != null)
			{
				int position = alphaIndexer.get(s);
				mCityLit.setSelection(position);
				overlay.setText(s);
				overlay.setVisibility(View.VISIBLE);
				handler.removeCallbacks(overlayThread);
				// 延迟一秒后执行，让overlay为不可见
				handler.postDelayed(overlayThread, 1500);
			}
		}

	}

	// 设置overlay不可见
	private class OverlayThread implements Runnable
	{

		@Override
		public void run()
		{
			overlay.setVisibility(View.GONE);
		}

	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		windowManager.removeView(overlay);
		super.onDestroy();
	}

	
	
}
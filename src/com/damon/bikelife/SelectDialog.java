package com.damon.bikelife;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.baidu.mobads.appoffers.OffersManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class SelectDialog extends AlertDialog{
	
	private ListView menuList = null;
	public SelectDialog(Context context, int theme) {
	    super(context, theme);
	}
	
	public SelectDialog(Context context) {
	    super(context);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.menu);
	    
	    
	    
	    menuList = (ListView)findViewById(R.id.menu_list);
	    List<Map<String, Object>> list = new ArrayList<Map<String,Object>>();
	    Map<String,Object> map = new HashMap<String, Object>();
	    map.put("name", "应用游戏");
	    map.put("img", R.drawable.app);
	    list.add(map);
	    map = new HashMap<String, Object>();
	    map.put("name", "关于");
	    map.put("img", R.drawable.about);
	    list.add(map);
    	ListAdapter listAdapter = new ListAdapter(getContext(), list);
    	menuList.setAdapter(listAdapter);
    	menuList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// TODO Auto-generated method stub
				switch (arg2) {
				case 0:
					Intent intentad = new Intent();
					   intentad.setAction("com.damon.action.openadwall");
					   getContext().sendBroadcast(intentad);
					dismiss();					
					break;
				case 1:
					Intent intent = new Intent(getContext(),AboutActivity.class);
					getContext().startActivity(intent);
					dismiss();
					break;
				default:
					break;
				}
			}
		});
	}
	
	
	/**
	 * ListViewAdapter
	 * 
	 * @author damon
	 */
	private class ListAdapter extends BaseAdapter
	{
		private LayoutInflater inflater;
		private List<Map<String, Object>> list;

		public ListAdapter(Context context, List<Map<String, Object>> list)
		{

			this.inflater = LayoutInflater.from(context);
			this.list = list;

		}

		public int getCount()
		{
			return list.size();
		}

		public Object getItem(int position)
		{
			return list.get(position);
		}

		public long getItemId(int position)
		{
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent)
		{
			ViewHolder holder;
			if (convertView == null)
			{
				convertView = inflater.inflate(R.layout.menu_item, null);
				holder = new ViewHolder();
				holder.img = (ImageView) convertView.findViewById(R.id.menu_img);
				holder.name = (TextView) convertView.findViewById(R.id.menu_name);
				convertView.setTag(holder);
			} else
			{
				holder = (ViewHolder) convertView.getTag();
			}
			Map<String, Object> obj = (Map<String, Object>)list.get(position);
			holder.name.setText((String)obj.get("name"));
			holder.img.setImageResource((Integer)obj.get("img"));			
			return convertView;
		}

		private class ViewHolder
		{
			ImageView img;
			TextView name;
		}

	}
}

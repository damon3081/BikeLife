package com.damon.bikelife.utils;

import java.io.InputStream;

import org.apache.http.util.EncodingUtils;

import android.content.Context;

public class Utils {
	
	public static String getFromAssets(Context context,String fileName){ 
    	String result = "";
    	   try {
    	InputStream in = context.getResources().getAssets().open(fileName);
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

}

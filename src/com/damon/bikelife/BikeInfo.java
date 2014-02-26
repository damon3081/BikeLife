package com.damon.bikelife;

import com.baidu.mapapi.map.LocationData;


public class BikeInfo {
	
	private String cityName;
	private String fileName;
	private String url;
	private LocationData locData;
	private String geoType;
	public BikeInfo(String fileName, String url, LocationData locData) {
		super();
		this.fileName = fileName;
		this.url = url;
		this.locData = locData;
		this.geoType = "bd09ll";
	}
	
	
	public BikeInfo(String cityName,String fileName, String url, LocationData locData,
			String geoType) {
		super();
		this.cityName  = cityName;
		this.fileName = fileName;
		this.url = url;
		this.locData = locData;
		this.geoType = geoType;
	}


	public String getCityName() {
		return cityName;
	}


	public void setCityName(String cityName) {
		this.cityName = cityName;
	}


	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public LocationData getLocData() {
		return locData;
	}
	public void setLocData(LocationData locData) {
		this.locData = locData;
	}
	public String getGeoType() {
		return geoType;
	}
	public void setGeoType(String geoType) {
		this.geoType = geoType;
	}
    
	
	
}

package com.damon.bikelife.city;

import java.util.Comparator;

/**
 * 城市属性实体类
 * @author damon
 *
 */
public class CityModel 
{
	private String CityName; //城市名称
	private String NameSort; //城市首字母
	
	public CityModel() {
		super();
	}
	
	public CityModel(String cityName, String nameSort) {
		super();
		CityName = cityName;
		NameSort = nameSort;
	}

	public String getCityName()
	{
		return CityName;
	}

	public void setCityName(String cityName)
	{
		CityName = cityName;
	}

	public String getNameSort()
	{
		return NameSort;
	}

	public void setNameSort(String nameSort)
	{
		NameSort = nameSort;
	}

	public static Comparator<CityModel> comparator = new Comparator<CityModel>(){
		   public int compare(CityModel s1, CityModel s2) {
				//int result = ? 1 : (s1.getNameSort().equals(s2.getNameSort())  ? 0 : -1);              
				return s1.getNameSort().compareTo(s2.getNameSort());
		   }
	};

}

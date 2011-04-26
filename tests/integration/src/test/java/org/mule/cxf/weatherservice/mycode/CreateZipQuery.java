package org.mule.cxf.weatherservice.mycode;

import org.mule.cxf.weatherservice.myweather.GetCityWeatherByZIP;


public class CreateZipQuery
{

	/**
	 * Create a request to query by zip code.
	 *
	 * @param input
	 * @return
	 */
	public GetCityWeatherByZIP createRequest(Object input) {
		GetCityWeatherByZIP request = new GetCityWeatherByZIP();
		request.setZIP("30075");
		return request;
	}
}
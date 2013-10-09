/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.cxf.weatherservice.mycode;

import org.mule.cxf.weatherservice.myweather.GetCityWeatherByZIP;


public class CreateZipQuery
{

    /**
     * Create a request to query by zip code.
     */
    public GetCityWeatherByZIP createRequest(Object input) {
        GetCityWeatherByZIP request = new GetCityWeatherByZIP();
        request.setZIP("30075");
        return request;
    }
}

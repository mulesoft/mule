/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.cxf.weatherservice.myweather;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.ParameterStyle;
import javax.xml.bind.annotation.XmlSeeAlso;

@WebService(targetNamespace = "http://ws.cdyne.com/WeatherWS/", name = "WeatherSoap")
@XmlSeeAlso({ObjectFactory.class})
@SOAPBinding(parameterStyle = ParameterStyle.BARE)
public interface WeatherSoap {

    @WebResult(name = "GetCityWeatherByZIPResponse", targetNamespace = "http://ws.cdyne.com/WeatherWS/", partName = "parameters")
    @WebMethod(operationName = "GetCityWeatherByZIP", action = "http://ws.cdyne.com/WeatherWS/GetCityWeatherByZIP")
    public GetCityWeatherByZIPResponse getCityWeatherByZIP(
        @WebParam(partName = "parameters", name = "GetCityWeatherByZIP", targetNamespace = "http://ws.cdyne.com/WeatherWS/")
        GetCityWeatherByZIP parameters
    );

    @WebResult(name = "GetWeatherInformationResponse", targetNamespace = "http://ws.cdyne.com/WeatherWS/", partName = "parameters")
    @WebMethod(operationName = "GetWeatherInformation", action = "http://ws.cdyne.com/WeatherWS/GetWeatherInformation")
    public GetWeatherInformationResponse getWeatherInformation(
        @WebParam(partName = "parameters", name = "GetWeatherInformation", targetNamespace = "http://ws.cdyne.com/WeatherWS/")
        GetWeatherInformation parameters
    );

    @WebResult(name = "GetCityForecastByZIPResponse", targetNamespace = "http://ws.cdyne.com/WeatherWS/", partName = "parameters")
    @WebMethod(operationName = "GetCityForecastByZIP", action = "http://ws.cdyne.com/WeatherWS/GetCityForecastByZIP")
    public GetCityForecastByZIPResponse getCityForecastByZIP(
        @WebParam(partName = "parameters", name = "GetCityForecastByZIP", targetNamespace = "http://ws.cdyne.com/WeatherWS/")
        GetCityForecastByZIP parameters
    );
}
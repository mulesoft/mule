/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
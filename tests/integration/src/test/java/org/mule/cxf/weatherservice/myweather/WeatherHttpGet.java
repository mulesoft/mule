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

@WebService(targetNamespace = "http://ws.cdyne.com/WeatherWS/", name = "WeatherHttpGet")
@XmlSeeAlso({ObjectFactory.class})
@SOAPBinding(parameterStyle = ParameterStyle.BARE)
public interface WeatherHttpGet {

    @WebResult(name = "WeatherReturn", targetNamespace = "http://ws.cdyne.com/WeatherWS/", partName = "Body")
    @WebMethod(operationName = "GetCityWeatherByZIP")
    public WeatherReturn getCityWeatherByZIP(
        @WebParam(partName = "ZIP", name = "ZIP", targetNamespace = "")
        String zip
    );

    @WebResult(name = "ArrayOfWeatherDescription", targetNamespace = "http://ws.cdyne.com/WeatherWS/", partName = "Body")
    @WebMethod(operationName = "GetWeatherInformation")
    public ArrayOfWeatherDescription getWeatherInformation();

    @WebResult(name = "ForecastReturn", targetNamespace = "http://ws.cdyne.com/WeatherWS/", partName = "Body")
    @WebMethod(operationName = "GetCityForecastByZIP")
    public ForecastReturn getCityForecastByZIP(
        @WebParam(partName = "ZIP", name = "ZIP", targetNamespace = "")
        String zip
    );
}
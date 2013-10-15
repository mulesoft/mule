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
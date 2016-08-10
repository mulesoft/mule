/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.cxf.weatherservice.myweather;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;

@WebServiceClient(name = "Weather",
                  wsdlLocation = "c:/tmp/weather.wsdl",
                  targetNamespace = "http://ws.cdyne.com/WeatherWS/")
public class Weather extends Service {

    public final static URL WSDL_LOCATION;
    public final static QName SERVICE = new QName("http://ws.cdyne.com/WeatherWS/", "Weather");
    public final static QName WeatherHttpPost = new QName("http://ws.cdyne.com/WeatherWS/", "WeatherHttpPost");
    public final static QName WeatherHttpGet = new QName("http://ws.cdyne.com/WeatherWS/", "WeatherHttpGet");
    public final static QName WeatherSoap12 = new QName("http://ws.cdyne.com/WeatherWS/", "WeatherSoap12");
    public final static QName WeatherSoap = new QName("http://ws.cdyne.com/WeatherWS/", "WeatherSoap");
    static
    {
        WSDL_LOCATION = Weather.class.getClassLoader().getResource("org/mule/issues/weather.wsdl");
    }

    public Weather(URL wsdlLocation) {
        super(wsdlLocation, SERVICE);
    }

    public Weather(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public Weather() {
        super(WSDL_LOCATION, SERVICE);
    }

    /**
     *
     * @return
     *     returns WeatherHttpPost
     */
    @WebEndpoint(name = "WeatherHttpPost")
    public WeatherHttpPost getWeatherHttpPost() {
        return super.getPort(WeatherHttpPost, WeatherHttpPost.class);
    }

    /**
     *
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns WeatherHttpPost
     */
    @WebEndpoint(name = "WeatherHttpPost")
    public WeatherHttpPost getWeatherHttpPost(WebServiceFeature... features) {
        return super.getPort(WeatherHttpPost, WeatherHttpPost.class, features);
    }
    /**
     *
     * @return
     *     returns WeatherHttpGet
     */
    @WebEndpoint(name = "WeatherHttpGet")
    public WeatherHttpGet getWeatherHttpGet() {
        return super.getPort(WeatherHttpGet, WeatherHttpGet.class);
    }

    /**
     *
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns WeatherHttpGet
     */
    @WebEndpoint(name = "WeatherHttpGet")
    public WeatherHttpGet getWeatherHttpGet(WebServiceFeature... features) {
        return super.getPort(WeatherHttpGet, WeatherHttpGet.class, features);
    }
    /**
     *
     * @return
     *     returns WeatherSoap
     */
    @WebEndpoint(name = "WeatherSoap12")
    public WeatherSoap getWeatherSoap12() {
        return super.getPort(WeatherSoap12, WeatherSoap.class);
    }

    /**
     *
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns WeatherSoap
     */
    @WebEndpoint(name = "WeatherSoap12")
    public WeatherSoap getWeatherSoap12(WebServiceFeature... features) {
        return super.getPort(WeatherSoap12, WeatherSoap.class, features);
    }
    /**
     *
     * @return
     *     returns WeatherSoap
     */
    @WebEndpoint(name = "WeatherSoap")
    public WeatherSoap getWeatherSoap() {
        return super.getPort(WeatherSoap, WeatherSoap.class);
    }

    /**
     *
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns WeatherSoap
     */
    @WebEndpoint(name = "WeatherSoap")
    public WeatherSoap getWeatherSoap(WebServiceFeature... features) {
        return super.getPort(WeatherSoap, WeatherSoap.class, features);
    }

}
/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.cxf.weatherservice.myweather;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;

public final class WeatherSoap_WeatherSoap_Client {

    private static final QName SERVICE_NAME = new QName("http://ws.cdyne.com/WeatherWS/", "Weather");

    public static void main(String args[]) throws Exception {
        URL wsdlURL = Weather.WSDL_LOCATION;
        if (args.length > 0) {
            File wsdlFile = new File(args[0]);
            try {
                if (wsdlFile.exists()) {
                    wsdlURL = wsdlFile.toURI().toURL();
                } else {
                    wsdlURL = new URL(args[0]);
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }

        Weather ss = new Weather(wsdlURL, SERVICE_NAME);
        WeatherSoap port = ss.getWeatherSoap();

        {
        System.out.println("Invoking getCityWeatherByZIP...");
        GetCityWeatherByZIP _getCityWeatherByZIP_parameters = null;
        GetCityWeatherByZIPResponse _getCityWeatherByZIP__return = port.getCityWeatherByZIP(_getCityWeatherByZIP_parameters);
        System.out.println("getCityWeatherByZIP.result=" + _getCityWeatherByZIP__return);


        }
        {
        System.out.println("Invoking getWeatherInformation...");
        GetWeatherInformation _getWeatherInformation_parameters = null;
        GetWeatherInformationResponse _getWeatherInformation__return = port.getWeatherInformation(_getWeatherInformation_parameters);
        System.out.println("getWeatherInformation.result=" + _getWeatherInformation__return);


        }
        {
        System.out.println("Invoking getCityForecastByZIP...");
        GetCityForecastByZIP _getCityForecastByZIP_parameters = null;
        GetCityForecastByZIPResponse _getCityForecastByZIP__return = port.getCityForecastByZIP(_getCityForecastByZIP_parameters);
        System.out.println("getCityForecastByZIP.result=" + _getCityForecastByZIP__return);


        }

        System.exit(0);
    }

}
/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.cxf.weatherservice.myweather;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;

public final class WeatherHttpPost_WeatherHttpPost_Client {

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
        WeatherHttpPost port = ss.getWeatherHttpPost();

        {
        System.out.println("Invoking getCityWeatherByZIP...");
        String _getCityWeatherByZIP_zip = "";
        WeatherReturn _getCityWeatherByZIP__return = port.getCityWeatherByZIP(_getCityWeatherByZIP_zip);
        System.out.println("getCityWeatherByZIP.result=" + _getCityWeatherByZIP__return);


        }
        {
        System.out.println("Invoking getWeatherInformation...");
        ArrayOfWeatherDescription _getWeatherInformation__return = port.getWeatherInformation();
        System.out.println("getWeatherInformation.result=" + _getWeatherInformation__return);


        }
        {
        System.out.println("Invoking getCityForecastByZIP...");
        String _getCityForecastByZIP_zip = "";
        ForecastReturn _getCityForecastByZIP__return = port.getCityForecastByZIP(_getCityForecastByZIP_zip);
        System.out.println("getCityForecastByZIP.result=" + _getCityForecastByZIP__return);


        }

        System.exit(0);
    }

}
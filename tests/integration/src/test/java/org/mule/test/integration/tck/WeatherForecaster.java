/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.tck;

import javax.jws.WebMethod;
import javax.jws.WebService;

@WebService(serviceName = "WeatherForecaster", portName = "WeatherForecasterPort")
public class WeatherForecaster
{
    @WebMethod(operationName = "GetWeatherByZipCode")
    public String getByZipCode(String zipCode)
    {
        return zipCode + ": cloudy with chances of meatballs.";
    }
}

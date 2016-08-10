/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.issues;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.module.http.api.HttpConstants.Methods.POST;
import static org.mule.runtime.module.http.api.client.HttpRequestOptionsBuilder.newOptions;

import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.lifecycle.Callable;
import org.mule.runtime.core.transformer.simple.ObjectToString;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

@Ignore("See MULE-9196")
public class HttpReturnsJaxbObject5531TestCase extends AbstractIntegrationTestCase
{
    private static final String ZIP_RESPONSE = "<?xml version='1.0' encoding='utf-8'?><soap:Envelope xmlns:soap='http://schemas.xmlsoap.org/soap/envelope/' "
                                               + "xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:xsd='http://www.w3.org/2001/XMLSchema'>"
                                               + "<soap:Body><GetCityWeatherByZIPResponse xmlns='http://ws.cdyne.com/WeatherWS/'><GetCityWeatherByZIPResult>"
                                               + "<Success>true</Success><ResponseText>City Found</ResponseText><State>GA</State><City>Roswell</City>"
                                               + "<WeatherStationCity>Marietta</WeatherStationCity><WeatherID>1</WeatherID><Description>Thunder Storms</Description>"
                                               + "<Temperature>79</Temperature><RelativeHumidity>57</RelativeHumidity><Wind>S8</Wind>"
                                               + "<Pressure>29.91R</Pressure><Visibility /><WindChill /><Remarks /></GetCityWeatherByZIPResult>"
                                               + "</GetCityWeatherByZIPResponse></soap:Body></soap:Envelope>";

    @Rule
    public DynamicPort port1 = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "org/mule/issues/http-returns-jaxb-object-mule-5531-test.xml";
    }

    @Test
    public void testGetWeather() throws Exception
    {
        String testUrl = "http://localhost:" + port1.getNumber() + "/test";
        MuleClient client = muleContext.getClient();
        Object response = client.send(testUrl, getTestMuleMessage("hello"), newOptions().method(POST.name()).build());
        assertNotNull(response);
        String stringResponse = (String) new ObjectToString().transform(response, UTF_8);
        assertTrue(stringResponse.contains("<Success>true</Success>"));
    }

    public static class WeatherReport implements Callable
    {
        @Override
        public Object onCall(MuleEventContext eventContext) throws Exception
        {
            return ZIP_RESPONSE;
        }
    }
}

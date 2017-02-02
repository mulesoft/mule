/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.issues;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertTrue;
import static org.mule.service.http.api.HttpConstants.Method.POST;
import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.lifecycle.Callable;
import org.mule.runtime.core.util.IOUtils;
import org.mule.service.http.api.HttpService;
import org.mule.service.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.domain.message.response.HttpResponse;
import org.mule.services.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

@Ignore("See MULE-9196")
public class HttpReturnsJaxbObject5531TestCase extends AbstractIntegrationTestCase {

  private static final String ZIP_RESPONSE =
      "<?xml version='1.0' encoding='utf-8'?><soap:Envelope xmlns:soap='http://schemas.xmlsoap.org/soap/envelope/' "
          + "xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:xsd='http://www.w3.org/2001/XMLSchema'>"
          + "<soap:Body><GetCityWeatherByZIPResponse xmlns='http://ws.cdyne.com/WeatherWS/'><GetCityWeatherByZIPResult>"
          + "<Success>true</Success><ResponseText>City Found</ResponseText><State>GA</State><City>Roswell</City>"
          + "<WeatherStationCity>Marietta</WeatherStationCity><WeatherID>1</WeatherID><Description>Thunder Storms</Description>"
          + "<Temperature>79</Temperature><RelativeHumidity>57</RelativeHumidity><Wind>S8</Wind>"
          + "<Pressure>29.91R</Pressure><Visibility /><WindChill /><Remarks /></GetCityWeatherByZIPResult>"
          + "</GetCityWeatherByZIPResponse></soap:Body></soap:Envelope>";

  @Rule
  public DynamicPort port1 = new DynamicPort("port1");

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder(getService(HttpService.class)).build();

  @Override
  protected String getConfigFile() {
    return "org/mule/issues/http-returns-jaxb-object-mule-5531-test.xml";
  }

  @Test
  public void testGetWeather() throws Exception {
    HttpRequest request =
        HttpRequest.builder().setUri("http://localhost:" + port1.getNumber() + "/test").setMethod(POST).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);
    String payload = IOUtils.toString(((InputStreamHttpEntity) response.getEntity()).getInputStream(), UTF_8);
    assertTrue(payload.contains("<Success>true</Success>"));
  }

  public static class WeatherReport implements Callable {

    @Override
    public Object onCall(MuleEventContext eventContext) throws Exception {
      return ZIP_RESPONSE;
    }
  }
}

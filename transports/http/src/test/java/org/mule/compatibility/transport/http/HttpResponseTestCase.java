/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.Serializable;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.Cookie;
import org.junit.Rule;
import org.junit.Test;


public class HttpResponseTestCase extends FunctionalTestCase {

  private static final String HTTP_BODY = "<html><head></head><body><p>This is the response body</p></body></html>";

  @Rule
  public DynamicPort dynamicPort1 = new DynamicPort("port1");

  @Rule
  public DynamicPort dynamicPort2 = new DynamicPort("port2");

  @Override
  protected String getConfigFile() {
    return "http-response-conf.xml";
  }

  @Test
  public void testHttpResponseError() throws Exception {
    MuleClient client = muleContext.getClient();
    Map<String, Serializable> properties = new HashMap<>();
    properties.put("errorMessage", "ERROR !!!! ");
    MuleMessage muleMessage = MuleMessage.builder().payload(HTTP_BODY).outboundProperties(properties).build();
    MuleMessage response =
        client.send("http://localhost:" + dynamicPort1.getNumber() + "/resources/error", muleMessage).getRight();
    assertTrue(getPayloadAsString(response).contains("ERROR !!!!"));
    assertEquals("" + HttpConstants.SC_INTERNAL_SERVER_ERROR, response.getInboundProperty("http.status"));
  }

  @Test
  public void testHttpResponseMove() throws Exception {
    MuleClient client = muleContext.getClient();
    MuleMessage muleMessage = getTestMuleMessage(HTTP_BODY);
    MuleMessage response =
        client.send("http://localhost:" + dynamicPort1.getNumber() + "/resources/move", muleMessage).getRight();
    assertEquals(HTTP_BODY, getPayloadAsString(response));
    assertEquals("" + HttpConstants.SC_MOVED_PERMANENTLY, response.getInboundProperty("http.status"));
    assertEquals("http://localhost:9090/resources/moved", response.getInboundProperty("Location"));
  }

  @Test
  public void testHttpResponseAll() throws Exception {
    MuleClient client = muleContext.getClient();
    MuleMessage muleMessage = getTestMuleMessage(HTTP_BODY);
    MuleMessage response = client.send("http://localhost:" + dynamicPort1.getNumber() + "/resources/all", muleMessage).getRight();
    assertEquals("Custom body", getPayloadAsString(response));
    assertEquals("" + HttpConstants.SC_NOT_FOUND, response.getInboundProperty("http.status"));
    assertEquals("public,no-cache,must-revalidate,max-age=3600,no-transform", response.getInboundProperty("Cache-Control"));
    assertEquals("Thu, 01 Dec 2014 16:00:00 GMT", response.getInboundProperty("Expires"));
    assertEquals("http://localhost:9090", response.getInboundProperty("Location"));
    assertEquals("value1", response.getInboundProperty("header1"));
    Cookie[] cookies = (Cookie[]) response.getInboundProperty("Set-Cookie");
    assertEquals(2, cookies.length);
    validateCookie(cookies[0]);
    validateCookie(cookies[1]);
  }

  @Test
  public void testHttpResponseAllWithExpressions() throws Exception {
    MuleClient client = muleContext.getClient();
    Map<String, Serializable> properties = populateProperties();

    MuleMessage muleMessage = MuleMessage.builder().payload(HTTP_BODY).outboundProperties(properties).build();
    MuleMessage response =
        client.send("http://localhost:" + dynamicPort1.getNumber() + "/resources/allExpressions", muleMessage).getRight();
    assertEquals("" + HttpConstants.SC_NOT_FOUND, response.getInboundProperty("http.status"));
    assertEquals("max-age=3600", response.getInboundProperty("Cache-Control"));
    assertEquals("Thu, 01 Dec 2014 16:00:00 GMT", response.getInboundProperty("Expires"));
    assertEquals("http://localhost:9090", response.getInboundProperty("Location"));
    assertEquals("value1", response.getInboundProperty("header1"));
    Cookie[] cookies = (Cookie[]) response.getInboundProperty("Set-Cookie");
    assertEquals(2, cookies.length);
    validateCookie(cookies[0]);
    validateCookie(cookies[1]);
  }

  @Test
  public void headersAreAddedWithNestedResponseBuilderFromHttpModule() throws Exception {
    assertHeaderInResponse("http://localhost:" + dynamicPort2.getNumber() + "/nested");
  }

  @Test
  public void headersAreAddedWithGlobalResponseBuilderFromHttpModule() throws Exception {
    assertHeaderInResponse("http://localhost:" + dynamicPort2.getNumber() + "/global");
  }

  @Test
  public void headersAreAddedWithNestedErrorResponseBuilderFromHttpModule() throws Exception {
    assertHeaderInResponse("http://localhost:" + dynamicPort2.getNumber() + "/nestedError");
  }

  @Test
  public void headersAreAddedWithGlobalErrorResponseBuilderFromHttpModule() throws Exception {
    assertHeaderInResponse("http://localhost:" + dynamicPort2.getNumber() + "/globalError");
  }


  private void assertHeaderInResponse(String url) throws Exception {
    MuleClient client = muleContext.getClient();
    MuleMessage response = client.request(url, RECEIVE_TIMEOUT).getRight().get();
    assertThat((String) response.getOutboundProperty("testHeader"), equalTo("testValue"));
  }

  private Map<String, Serializable> populateProperties() {
    Map<String, Serializable> properties = new HashMap<>();
    properties.put("customBody", "Custom body");
    properties.put("contentType", "text/html");
    properties.put("status", HttpConstants.SC_NOT_FOUND);
    properties.put("cacheControl", "3600");
    properties.put("expires", "Thu, 01 Dec 2014 16:00:00 GMT");
    properties.put("location", "http://localhost:9090");
    properties.put("header1", "header1");
    properties.put("header2", "header2");
    properties.put("value1", "value1");
    properties.put("value2", "value2");
    properties.put("cookie1", "cookie1");
    properties.put("cookie2", "cookie2");
    properties.put("domain", "localhost");
    properties.put("path", "/");
    properties.put("secure", true);
    properties.put("expiryDate", "Fri, 12 Dec 2014 17:00:00 GMT");
    properties.put("maxAge", "1000");
    return properties;

  }

  private void validateCookie(Cookie cookie) {
    if ("cookie1".equals(cookie.getName())) {
      assertEquals("value1", cookie.getValue());
      assertEquals("/", cookie.getPath());
      assertEquals("localhost", cookie.getDomain());
      validateDate(cookie.getExpiryDate());
      assertTrue(cookie.getSecure());
    } else {
      assertEquals("cookie2", cookie.getName());
      assertEquals("value2", cookie.getValue());
      assertFalse(cookie.getSecure());
    }
  }

  private void validateDate(Date date) {
    GregorianCalendar cookieDate = new GregorianCalendar();
    cookieDate.setTime(date);

    assertEquals(2014, cookieDate.get(GregorianCalendar.YEAR));
    assertEquals(12, cookieDate.get(GregorianCalendar.DAY_OF_MONTH));
    assertEquals(11, cookieDate.get(GregorianCalendar.MONTH));
  }
}

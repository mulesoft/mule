/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.functional;

import static org.junit.Assert.assertEquals;

import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.transport.http.HttpConstants;
import org.mule.compatibility.transport.http.PatchMethod;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.construct.Flow;
import org.mule.tck.junit4.rule.DynamicPort;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.OptionsMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.TraceMethod;
import org.junit.ClassRule;
import org.junit.Test;

public class HttpMethodTestCase extends FunctionalTestCase {

  @ClassRule
  public static DynamicPort dynamicPort = new DynamicPort("port1");

  private HttpClient client;

  public HttpMethodTestCase() {
    setDisposeContextPerClass(true);
    client = new HttpClient();
  }

  @Override
  protected String getConfigFile() {
    return "http-method-test-flow.xml";
  }

  @Test
  public void testHead() throws Exception {
    HeadMethod method = new HeadMethod(getHttpEndpointAddress());
    int statusCode = client.executeMethod(method);
    assertEquals(HttpStatus.SC_OK, statusCode);
  }

  @Test
  public void testOptions() throws Exception {
    OptionsMethod method = new OptionsMethod(getHttpEndpointAddress());
    int statusCode = client.executeMethod(method);
    assertEquals(HttpStatus.SC_OK, statusCode);
  }

  @Test
  public void testPut() throws Exception {
    PutMethod method = new PutMethod(getHttpEndpointAddress());
    int statusCode = client.executeMethod(method);
    assertEquals(HttpStatus.SC_OK, statusCode);
  }

  @Test
  public void testDelete() throws Exception {
    DeleteMethod method = new DeleteMethod(getHttpEndpointAddress());
    int statusCode = client.executeMethod(method);
    assertEquals(HttpStatus.SC_OK, statusCode);
  }

  @Test
  public void testTrace() throws Exception {
    TraceMethod method = new TraceMethod(getHttpEndpointAddress());
    int statusCode = client.executeMethod(method);
    assertEquals(HttpStatus.SC_OK, statusCode);
  }

  @Test
  public void testConnect() throws Exception {
    CustomHttpMethod method = new CustomHttpMethod(HttpConstants.METHOD_CONNECT, getHttpEndpointAddress());
    int statusCode = client.executeMethod(method);
    assertEquals(HttpStatus.SC_OK, statusCode);
  }

  @Test
  public void testPatch() throws Exception {
    PatchMethod method = new PatchMethod(getHttpEndpointAddress());
    int statusCode = client.executeMethod(method);
    assertEquals(HttpStatus.SC_OK, statusCode);
  }

  @Test
  public void testFoo() throws Exception {
    CustomHttpMethod method = new CustomHttpMethod("FOO", getHttpEndpointAddress());
    int statusCode = client.executeMethod(method);
    assertEquals(HttpStatus.SC_BAD_REQUEST, statusCode);
  }

  private String getHttpEndpointAddress() {
    InboundEndpoint httpEndpoint = (InboundEndpoint) ((Flow) muleContext.getRegistry().lookupObject("httpIn")).getMessageSource();
    return httpEndpoint.getAddress();
  }

  private static class CustomHttpMethod extends HttpMethodBase {

    private final String method;

    public CustomHttpMethod(String method, String url) {
      super(url);
      this.method = method;
    }

    @Override
    public String getName() {
      return method;
    }
  }
}

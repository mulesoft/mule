/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.listener;

import static org.apache.http.HttpVersion.HTTP_1_0;

import org.apache.http.HttpVersion;
import org.junit.Test;

public class HttpListenerMapResponseStreaming10TestCase extends HttpListenerResponseStreamingTestCase
{

  @Override
  protected HttpVersion getHttpVersion()
  {
    return HTTP_1_0;
  }

  @Override
  protected String getConfigFile()
  {
    return "http-listener-map-response-streaming-config.xml";
  }

  @Test
  public void map() throws Exception
  {
    final String url = getUrl("map");
    testResponseIsContentLengthEncoding(url, getHttpVersion(), TEST_BODY_MAP);
  }

  @Test
  public void alwaysMap() throws Exception
  {
    final String url = getUrl("alwaysMap");
    testResponseIsNotChunkedEncoding(url, getHttpVersion(), TEST_BODY_MAP);
  }

  @Test
  public void neverMap() throws Exception
  {
    final String url = getUrl("neverMap");
    testResponseIsContentLengthEncoding(url, getHttpVersion(), TEST_BODY_MAP);
  }

}

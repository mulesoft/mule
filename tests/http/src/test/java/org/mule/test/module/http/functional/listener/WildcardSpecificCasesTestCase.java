/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.module.http.functional.listener;

import static org.mule.service.http.api.HttpConstants.HttpStatus.NOT_FOUND;
import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_EXTENSION;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.module.http.functional.AbstractHttpTestCase;

import java.io.IOException;

import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Features;

@Features(HTTP_EXTENSION)
public class WildcardSpecificCasesTestCase extends AbstractHttpTestCase {

  private static final String path1 = "prefix/*/suffix";
  private static final String path2 = "prefix/keyword/differentSuffix/";
  private static final String path3 = "/a/*/b";
  private static final String path4 = "/a/*/c/*/e/*";
  private static final String response = "OK";
  private static final String response1 = "First Listener invoked";
  private static final String response2 = "Second Listener invoked";
  private static final String URL = "http://localhost:%s/%s";

  @Rule
  public DynamicPort listenPort = new DynamicPort("port");

  @Rule
  public SystemProperty systemPropertyPath1 = new SystemProperty("path1", path1);

  @Rule
  public SystemProperty systemPropertyPath2 = new SystemProperty("path2", path2);

  @Rule
  public SystemProperty systemPropertyResponse1 = new SystemProperty("response1", response1);

  @Rule
  public SystemProperty systemPropertyResponse2 = new SystemProperty("response2", response2);

  @Rule
  public SystemProperty path3SystemProperty = new SystemProperty("path3", path3);

  @Rule
  public SystemProperty path4SystemProperty = new SystemProperty("path4", path4);

  @Rule
  public SystemProperty responseSystemProperty = new SystemProperty("response", response);

  @Override
  protected String getConfigFile() {
    return "wildcard-specific-cases-config.xml";
  }

  @Test
  public void testMiddlePathWildCard() throws IOException {
    final String url = format(URL, listenPort.getNumber(), "prefix/keyword/suffix");
    final Response httpResponse = Request.Get(url).execute();
    assertThat(httpResponse.returnContent().asString(), is(response1));
  }

  @Test
  public void testNullWildcard() throws Exception {
    final String url = format(URL, listenPort.getNumber(), "a/b");
    final Response httpResponse = Request.Get(url).execute();
    assertThat(httpResponse.returnResponse().getStatusLine().getStatusCode(), is(NOT_FOUND.getStatusCode()));
  }

  @Test
  public void testMultipleWildcards() throws Exception {
    final String url = format(URL, listenPort.getNumber(), "a/b/c/d/e/f");
    final Response httpResponse = Request.Get(url).execute();
    assertThat(httpResponse.returnContent().asString(), is(response));
  }
}

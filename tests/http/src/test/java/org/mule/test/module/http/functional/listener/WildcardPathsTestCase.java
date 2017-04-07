/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.module.http.functional.listener;

import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_EXTENSION;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.runners.Parameterized.Parameters;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.module.http.functional.AbstractHttpTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Collection;

import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;
import ru.yandex.qatools.allure.annotations.Features;

@RunnerDelegateTo(Parameterized.class)
@Features(HTTP_EXTENSION)
public class WildcardPathsTestCase extends AbstractHttpTestCase {

  private static final String response1 = "V1 Flow invoked";
  private static final String response2 = "V2 flow invoked";
  private static final String response3 = "V2 - Healthcheck";
  private static final String path1 = "/*";
  private static final String path2 = "V2/*";
  private static final String path3 = "V2/taxes/healthcheck";
  private static final String URL = "http://localhost:%s/%s";
  private final String path;
  private final String response;

  @Rule
  public DynamicPort listenPort = new DynamicPort("port");

  @Rule
  public SystemProperty path1SystemProperty = new SystemProperty("path1", path1);

  @Rule
  public SystemProperty path2SystemProperty = new SystemProperty("path2", path2);

  @Rule
  public SystemProperty path3SystemProperty = new SystemProperty("path3", path3);

  @Rule
  public SystemProperty response1SystemProperty = new SystemProperty("response1", response1);

  @Rule
  public SystemProperty response2systemProperty = new SystemProperty("response2", response2);

  @Rule
  public SystemProperty response3SystemProperty = new SystemProperty("response3", response3);


  public WildcardPathsTestCase(String path, String response) {
    this.path = path;
    this.response = response;
  }

  @Override
  protected String getConfigFile() {
    return "HttpTestPathsWildcard.xml";
  }

  @Parameters
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {"", response1}, {"taxes", response1},
        {"taxes/healtcheck", response1},
        {"taxes/1", response1},
        {"V2", response2},
        {"V2/taxes", response2},
        {"V2/console", response2},
        {"V2/taxes/1", response2},
        {"V2/taxes/healthcheck", response3}});
  }

  @Test
  public void testPath() throws Exception {
    final String url = String.format(URL, listenPort.getNumber(), path);
    final Response httpResponse = Request.Get(url).execute();
    assertThat(httpResponse.returnContent().asString(), is(response));
  }

}

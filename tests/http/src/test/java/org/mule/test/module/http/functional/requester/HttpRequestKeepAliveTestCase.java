/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.requester;

import static org.mule.service.http.api.HttpHeaders.Names.CONNECTION;
import static org.mule.service.http.api.HttpHeaders.Values.CLOSE;
import static org.mule.service.http.api.HttpHeaders.Values.KEEP_ALIVE;
import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_EXTENSION;
import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.extension.http.api.HttpRequestAttributes;
import org.mule.functional.junit4.FlowRunner;
import org.mule.runtime.core.util.StringUtils;
import org.mule.service.http.api.domain.ParameterMap;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Features;

@Features(HTTP_EXTENSION)
public class HttpRequestKeepAliveTestCase extends AbstractHttpRequestTestCase {

  @Override
  protected String getConfigFile() {
    return "http-request-keep-alive-config.xml";
  }

  @Test
  public void persistentRequestSendsKeepAliveHeader() throws Exception {
    assertConnectionHeader("persistentRequestFlow", null, KEEP_ALIVE);
  }

  @Test
  public void nonPersistentRequestSendsCloseHeader() throws Exception {
    assertConnectionHeader("nonPersistentRequestFlow", null, CLOSE);
  }

  @Test
  public void persistentRequestWithKeepAlivePropertySendsKeepAliveHeader() throws Exception {
    assertConnectionHeader("persistentRequestFlow", KEEP_ALIVE, KEEP_ALIVE);
  }

  @Test
  public void nonPersistentRequestWithKeepAlivePropertySendsCloseHeader() throws Exception {
    assertConnectionHeader("nonPersistentRequestFlow", KEEP_ALIVE, CLOSE);
  }

  @Test
  public void nonPersistentRequestWithClosePropertySendsCloseHeader() throws Exception {
    assertConnectionHeader("nonPersistentRequestFlow", CLOSE, CLOSE);
  }


  private void assertConnectionHeader(String flow, String connectionOutboundProperty, String expectedConnectionHeader)
      throws Exception {
    FlowRunner runner = flowRunner(flow).withPayload(TEST_MESSAGE);

    if (connectionOutboundProperty != null) {
      final HttpRequestAttributes reqAttributes = mock(HttpRequestAttributes.class);
      when(reqAttributes.getHeaders()).thenReturn(new ParameterMap(singletonMap(CONNECTION, connectionOutboundProperty)));

      runner = runner.withAttributes(reqAttributes);
    }
    runner.run();
    String responseConnectionHeaderValue = StringUtils.join(headers.get(CONNECTION), " ");
    assertThat(responseConnectionHeaderValue, equalTo(expectedConnectionHeader));
  }

}

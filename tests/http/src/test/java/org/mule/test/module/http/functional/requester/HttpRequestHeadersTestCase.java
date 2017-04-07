/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.requester;

import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.service.http.api.HttpHeaders.Names.CONNECTION;
import static org.mule.service.http.api.HttpHeaders.Names.HOST;
import static org.mule.service.http.api.HttpHeaders.Names.TRANSFER_ENCODING;
import static org.mule.service.http.api.HttpHeaders.Values.CHUNKED;
import static org.mule.service.http.api.HttpHeaders.Values.CLOSE;
import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_EXTENSION;

import org.mule.extension.http.api.HttpRequestAttributes;
import org.mule.service.http.api.domain.ParameterMap;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Features;

@Features(HTTP_EXTENSION)
public class HttpRequestHeadersTestCase extends AbstractHttpRequestTestCase {

  @Rule
  public SystemProperty host = new SystemProperty("host", "localhost");
  @Rule
  public SystemProperty encoding = new SystemProperty("encoding", CHUNKED);

  @Override
  protected String getConfigFile() {
    return "http-request-headers-config.xml";
  }

  @Test
  public void sendsHeadersFromList() throws Exception {
    flowRunner("headerList").withPayload(TEST_MESSAGE).withVariable("headerName", "testName2")
        .withVariable("headerValue", "testValue2").run();

    assertThat(getFirstReceivedHeader("testName1"), equalTo("testValue1"));
    assertThat(getFirstReceivedHeader("testName2"), equalTo("testValue2"));
  }

  @Test
  public void sendsHeadersFromMap() throws Exception {
    Map<String, String> params = new HashMap<>();
    params.put("testName1", "testValue1");
    params.put("testName2", "testValue2");
    flowRunner("headerMap").withPayload(TEST_MESSAGE).withVariable("headers", params).run();

    assertThat(getFirstReceivedHeader("testName1"), equalTo("testValue1"));
    assertThat(getFirstReceivedHeader("testName2"), equalTo("testValue2"));
  }

  @Ignore("MULE-11606: Support a way to have DW handle multimaps")
  @Test
  public void overridesHeaders() throws Exception {
    Map<String, String> params = new HashMap<>();
    params.put("testName1", "testValueNew");
    params.put("testName2", "testValue2");
    flowRunner("headerOverride").withPayload(TEST_MESSAGE).withVariable("headers", params).run();

    final Collection<String> values = headers.get("testName1");
    assertThat(values, Matchers.containsInAnyOrder(Arrays.asList("testValue1", "testValueNew").toArray(new String[2])));
    assertThat(getFirstReceivedHeader("testName2"), equalTo("testValue2"));
  }

  @Test
  public void allowsUserAgentOverride() throws Exception {
    Map<String, String> params = new HashMap<>();
    params.put("User-Agent", "TEST");
    flowRunner("headerMap").withPayload(TEST_MESSAGE).withVariable("headers", params).run();

    assertThat(getFirstReceivedHeader("User-Agent"), equalTo("TEST"));
  }

  @Test
  public void ignoresHttpOutboundPropertiesButAcceptsHeaders() throws Exception {
    final HttpRequestAttributes reqAttributes = mock(HttpRequestAttributes.class);
    when(reqAttributes.getListenerPath()).thenReturn("listenerPath");

    flowRunner("httpHeaders").withPayload(TEST_MESSAGE).withAttributes(reqAttributes).run();

    assertThat(getFirstReceivedHeader("http.scheme"), is("testValue1"));
    assertThat(headers.asMap(), not(hasKey("http.listener.path")));
  }

  @Test
  public void acceptsConnectionHeader() throws Exception {
    flowRunner("connectionHeader").withPayload(TEST_MESSAGE).run();
    assertThat(getFirstReceivedHeader(CONNECTION), is(CLOSE));
  }

  @Test
  public void ignoresConnectionOutboundProperty() throws Exception {
    final HttpRequestAttributes reqAttributes = mock(HttpRequestAttributes.class);
    when(reqAttributes.getHeaders()).thenReturn(new ParameterMap(singletonMap(CONNECTION, CLOSE)));

    flowRunner("outboundProperties").withPayload(TEST_MESSAGE).withAttributes(reqAttributes).run();
    assertThat(getFirstReceivedHeader(CONNECTION), is(not(CLOSE)));
  }

  @Test
  public void acceptsHostHeader() throws Exception {
    flowRunner("hostHeader").withPayload(TEST_MESSAGE).run();
    assertThat(getFirstReceivedHeader(HOST), is(host.getValue()));
  }

  @Test
  public void acceptsTransferEncodingHeader() throws Exception {
    flowRunner("transferEncodingHeader").withPayload(TEST_MESSAGE).run();
    assertThat(getFirstReceivedHeader(TRANSFER_ENCODING), is(encoding.getValue()));
  }
}

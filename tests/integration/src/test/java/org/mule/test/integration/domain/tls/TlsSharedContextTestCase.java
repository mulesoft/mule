/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.domain.tls;

import static java.lang.String.format;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.service.http.api.HttpConstants.Method.GET;

import org.mule.functional.junit4.DomainFunctionalTestCase;
import org.mule.functional.junit4.FlowRunner;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.util.IOUtils;
import org.mule.service.http.api.client.HttpClient;
import org.mule.service.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.service.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.domain.message.response.HttpResponse;
import org.mule.services.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

@Ignore("MULE-10633")
public class TlsSharedContextTestCase extends DomainFunctionalTestCase {

  private static final String DATA = "data";
  private static final String FIRST_APP = "firstApp";
  private static final String SECOND_APP = "secondApp";

  @Rule
  public DynamicPort port1 = new DynamicPort("port1");
  @Rule
  public DynamicPort port2 = new DynamicPort("port2");
  @Rule
  public DynamicPort port3 = new DynamicPort("port3");

  @Override
  protected String getDomainConfig() {
    return "domain/tls/tls-domain-config.xml";
  }

  @Override
  public ApplicationConfig[] getConfigResources() {
    return new ApplicationConfig[] {new ApplicationConfig(FIRST_APP, new String[] {"domain/tls/tls-first-app-config.xml"}),
        new ApplicationConfig(SECOND_APP, new String[] {"domain/tls/tls-second-app-config.xml"})};
  }

  @Test
  public void sharedRequesterUsingSharedTlsContextToLocalListener() throws Exception {
    testFlowForApp("helloWorldClientFlow", FIRST_APP, "hello world");
  }

  @Test
  public void localRequesterToSharedListenerUsingSharedTlsContext() throws Exception {
    testFlowForApp("helloMuleClientFlow", SECOND_APP, "hello mule");
  }

  @Test
  public void muleClientUsingSharedTlsContextToListenerUsingSharedTlsContext() throws Exception {
    MuleContext domainContext = getMuleContextForDomain();
    TlsContextFactory tlsContextFactory = domainContext.getRegistry().lookupObject("sharedTlsContext2");

    HttpClient httpClient = new TestHttpClient.Builder().tlsContextFactory(tlsContextFactory).build();
    httpClient.start();

    HttpRequest request = HttpRequest.builder().setUri(format("https://localhost:%s/helloAll", port3.getValue())).setMethod(GET)
        .setEntity(new ByteArrayHttpEntity(DATA.getBytes())).build();
    final HttpResponse response = httpClient.send(request, DEFAULT_TEST_TIMEOUT_SECS, false, null);

    httpClient.stop();

    assertThat(IOUtils.toString(((InputStreamHttpEntity) response.getEntity()).getInputStream()), is("hello all"));
  }

  private void testFlowForApp(String flowName, String appName, String expected) throws Exception {
    Event response = new FlowRunner(getMuleContextForApp(appName), flowName).withPayload(DATA).run();
    assertThat(response.getMessageAsString(getMuleContextForApp(appName)), is(expected));
  }
}

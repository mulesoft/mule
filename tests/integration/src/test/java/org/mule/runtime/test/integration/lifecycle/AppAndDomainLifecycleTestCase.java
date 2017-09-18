/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.test.integration.lifecycle;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.http.api.HttpConstants.Method.GET;

import org.mule.functional.junit4.ApplicationContextBuilder;
import org.mule.functional.junit4.DomainContextBuilder;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.mule.service.http.TestHttpClient;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Issue;

@Ignore("MULE-10633")
@Issue("MULE-10633")
public class AppAndDomainLifecycleTestCase extends AbstractMuleTestCase {

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");
  @Rule
  public SystemProperty endpointScheme = getEndpointSchemeSystemProperty();

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder().build();

  @Test
  public void appShutdownDoesNotStopsDomainConnector() throws Exception {
    MuleContext domainContext = null;
    MuleContext firstAppContext = null;
    MuleContext secondAppContext = null;
    try {
      domainContext = new DomainContextBuilder().setDomainConfig("lifecycle/domain/http/http-shared-listener-config.xml").build();
      firstAppContext = new ApplicationContextBuilder()
          .setApplicationResources(new String[] {"lifecycle/domain/http/http-hello-mule-app.xml"}).setDomainContext(domainContext)
          .build();
      ApplicationContextBuilder secondApp = new ApplicationContextBuilder();
      secondAppContext = secondApp.setApplicationResources(new String[] {"lifecycle/domain/http/http-hello-world-app.xml"})
          .setDomainContext(domainContext).build();
      firstAppContext.stop();

      HttpRequest request = HttpRequest.builder().uri("http://localhost:" + dynamicPort.getNumber() + "/service/helloWorld")
          .method(GET).entity(new ByteArrayHttpEntity("test".getBytes())).build();
      final HttpResponse response = httpClient.send(request, DEFAULT_TEST_TIMEOUT_SECS, false, null);

      assertThat(response, notNullValue());
      assertThat(IOUtils.toString(response.getEntity().getContent()), is("hello world"));
      // TODO MULE-10633 assert that the shared listener is started
    } finally {
      closeQuietly(domainContext);
      closeQuietly(firstAppContext);
      closeQuietly(secondAppContext);
    }
  }

  private void closeQuietly(MuleContext context) {
    if (context != null) {
      try {
        context.dispose();
      } catch (Exception e) {
        // Do nothing
      }
    }
  }

  public SystemProperty getEndpointSchemeSystemProperty() {
    return new SystemProperty("scheme", "http");
  }

}

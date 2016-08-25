/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.domain.http;

import static java.lang.String.format;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.mule.functional.junit4.DomainFunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.module.http.api.client.HttpRequestOptionsBuilder;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Rule;
import org.junit.Test;

public class HttpSharePortTestCase extends DomainFunctionalTestCase {

  public static final String HELLO_WORLD_SERVICE_APP = "helloWorldServiceApp";
  public static final String HELLO_MULE_SERVICE_APP = "helloMuleServiceApp";

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");
  @Rule
  public SystemProperty endpointScheme = getEndpointSchemeSystemProperty();

  @Override
  protected String getDomainConfig() {
    return "domain/http/http-shared-listener-config.xml";
  }

  @Override
  public ApplicationConfig[] getConfigResources() {
    return new ApplicationConfig[] {
        new ApplicationConfig(HELLO_WORLD_SERVICE_APP, new String[] {"domain/http/http-hello-world-app.xml"}),
        new ApplicationConfig(HELLO_MULE_SERVICE_APP, new String[] {"domain/http/http-hello-mule-app.xml"})};
  }

  @Test
  public void bothServicesBindCorrectly() throws Exception {
    MuleMessage helloWorldServiceResponse = getMuleContextForApp(HELLO_WORLD_SERVICE_APP).getClient()
        .send(format("%s://localhost:%d/service/helloWorld", endpointScheme.getValue(), dynamicPort.getNumber()),
              MuleMessage.builder().payload("test-data").build(), getOptionsBuilder().build())
        .getRight();
    assertThat(getPayloadAsString(helloWorldServiceResponse, getMuleContextForApp(HELLO_WORLD_SERVICE_APP)), is("hello world"));

    MuleMessage helloMuleServiceResponse = getMuleContextForApp(HELLO_MULE_SERVICE_APP).getClient()
        .send(format("%s://localhost:%d/service/helloMule", endpointScheme.getValue(), dynamicPort.getNumber()),
              MuleMessage.builder().payload("test-data").build(), getOptionsBuilder().build())
        .getRight();
    assertThat(getPayloadAsString(helloMuleServiceResponse, getMuleContextForApp(HELLO_MULE_SERVICE_APP)), is("hello mule"));
  }

  protected SystemProperty getEndpointSchemeSystemProperty() {
    return new SystemProperty("scheme", "http");
  }

  protected HttpRequestOptionsBuilder getOptionsBuilder() {
    return newOptions();
  }

}

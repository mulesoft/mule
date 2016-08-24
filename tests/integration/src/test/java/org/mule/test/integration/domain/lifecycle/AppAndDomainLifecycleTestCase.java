/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.domain.lifecycle;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import org.mule.functional.junit4.ApplicationContextBuilder;
import org.mule.functional.junit4.DomainContextBuilder;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.module.http.internal.listener.DefaultHttpListenerConfig;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Rule;
import org.junit.Test;

public class AppAndDomainLifecycleTestCase extends AbstractMuleTestCase {

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");
  @Rule
  public SystemProperty endpointScheme = getEndpointSchemeSystemProperty();

  @Test
  public void appShutdownDoesNotStopsDomainConnector() throws Exception {
    MuleContext domainContext = null;
    MuleContext firstAppContext = null;
    MuleContext secondAppContext = null;
    try {
      domainContext = new DomainContextBuilder().setDomainConfig("domain/http/http-shared-listener-config.xml").build();
      firstAppContext = new ApplicationContextBuilder()
          .setApplicationResources(new String[] {"domain/http/http-hello-mule-app.xml"}).setDomainContext(domainContext).build();
      ApplicationContextBuilder secondApp = new ApplicationContextBuilder();
      secondAppContext = secondApp.setApplicationResources(new String[] {"domain/http/http-hello-world-app.xml"})
          .setDomainContext(domainContext).build();
      firstAppContext.stop();
      MuleMessage response =
          secondAppContext.getClient().send("http://localhost:" + dynamicPort.getNumber() + "/service/helloWorld",
                                            MuleMessage.builder().payload("test").build())
              .getRight();
      assertThat(response, notNullValue());
      assertThat(secondAppContext.getTransformationService().transform(response, DataType.STRING).getPayload(),
                 is("hello world"));
      assertThat((domainContext.getRegistry().<DefaultHttpListenerConfig>get("sharedListenerConfig")).isStarted(), is(true));
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

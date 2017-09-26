/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.logging;

import static java.lang.String.format;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.privileged.registry.LegacyRegistryUtils.lookupObject;
import static org.mule.runtime.http.api.HttpConstants.Method.GET;

import org.mule.functional.listener.FlowExecutionListener;
import org.mule.runtime.api.notification.NotificationListenerRegistry;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.runtime.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.module.launcher.logging.rule.UseMuleLog4jContextFactory;
import org.mule.service.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.infrastructure.deployment.AbstractFakeMuleServerTestCase;
import org.mule.test.infrastructure.deployment.FakeMuleServer;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import io.qameta.allure.Issue;

@Ignore("MULE-10633, also, this uses FakeMuleServer which does not support loading extensions")
@Issue("MULE-10633")
public class ConnectorLevelMessageDispatchingTestCase extends AbstractFakeMuleServerTestCase {

  public static final String HELLO_WORLD_APP = "hello-world";
  public static final String HELLO_MULE_APP = "hello-mule";
  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");
  @Rule
  public SystemProperty endpointScheme = new SystemProperty("scheme", "http");
  @Rule
  public UseMuleLog4jContextFactory muleLogging = new UseMuleLog4jContextFactory();

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder().build();

  @Test
  public void verifyClassLoaderIsAppClassLoader() throws Exception {
    muleServer.deployDomainFromClasspathFolder("domain/deployable-domains/http-domain-listener", "domain");
    muleServer.deployAppFromClasspathFolder("domain/deployable-apps/hello-world-app", HELLO_WORLD_APP);
    muleServer.deployAppFromClasspathFolder("domain/deployable-apps/hello-mule-app", HELLO_MULE_APP);
    muleServer.start();
    verifyAppProcessMessageWithAppClassLoader(muleServer, HELLO_MULE_APP, "http://localhost:%d/service/helloMule");
    verifyAppProcessMessageWithAppClassLoader(muleServer, HELLO_WORLD_APP, "http://localhost:%d/service/helloWorld");
  }

  private void verifyAppProcessMessageWithAppClassLoader(FakeMuleServer fakeMuleServer, String appName, String requestUrl)
      throws IOException, TimeoutException, RegistrationException {
    MuleContext applicationContext = fakeMuleServer.findApplication(appName).getMuleContext();

    final AtomicReference<ClassLoader> executionClassLoader = new AtomicReference<>();
    FlowExecutionListener flowExecutionListener =
        new FlowExecutionListener(lookupObject(applicationContext, NotificationListenerRegistry.class));
    flowExecutionListener.addListener(source -> executionClassLoader.set(Thread.currentThread().getContextClassLoader()));
    HttpRequest request =
        HttpRequest.builder().uri(format(requestUrl, dynamicPort.getNumber())).method(GET)
            .entity(new ByteArrayHttpEntity("test-data".getBytes())).build();
    httpClient.send(request, DEFAULT_TEST_TIMEOUT_SECS, false, null);
    flowExecutionListener.waitUntilFlowIsComplete();
    assertThat(executionClassLoader.get(), is(applicationContext.getExecutionClassLoader()));
  }


}

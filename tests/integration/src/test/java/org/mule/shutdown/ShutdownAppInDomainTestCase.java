/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.shutdown;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.Event.getCurrentEvent;
import static org.mule.service.http.api.HttpConstants.Method.GET;
import org.mule.functional.junit4.DomainFunctionalTestCase;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.services.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.infrastructure.report.HeapDumpOnFailure;

import java.io.IOException;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests that threads in pools defined in a domain do not hold references to objects of the application in their thread locals.
 */
@Ignore("MULE-10335")
public class ShutdownAppInDomainTestCase extends DomainFunctionalTestCase {

  private static final int PROBER_POLLING_INTERVAL = 100;
  private static final int PROBER_POLIING_TIMEOUT = 5000;
  private static final int MESSAGE_TIMEOUT = 2000;

  @Rule
  public HeapDumpOnFailure heapDumpOnFailure = new HeapDumpOnFailure();

  private static final Set<PhantomReference<Event>> requestContextRefs = new HashSet<>();

  public static class RetrieveRequestContext implements Processor {

    @Override
    public Event process(Event event) throws MuleException {
      requestContextRefs.add(new PhantomReference<>(getCurrentEvent(), new ReferenceQueue<>()));
      return event;
    }
  }

  @Before
  public void before() {
    requestContextRefs.clear();
  }

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder().build();

  @Rule
  public DynamicPort httpPort = new DynamicPort("httpPort");

  @Override
  protected String getDomainConfig() {
    return "org/mule/shutdown/domain-with-connectors.xml";
  }

  @Override
  public ApplicationConfig[] getConfigResources() {
    return new ApplicationConfig[] {
        new ApplicationConfig("app-with-flows", new String[] {"org/mule/shutdown/app-with-flows.xml"})
    };
  }

  @Test
  public void httpListener() throws IOException, TimeoutException {
    MuleContext muleContextForApp = getMuleContextForApp("app-with-flows");

    HttpRequest request =
        HttpRequest.builder().setUri("http://localhost:" + httpPort.getNumber() + "/sync").setMethod(GET).build();
    httpClient.send(request, MESSAGE_TIMEOUT, false, null);

    muleContextForApp.dispose();

    assertEventsUnreferenced();
  }

  @Test
  public void httpListenerNonBlocking() throws IOException, TimeoutException {
    MuleContext muleContextForApp = getMuleContextForApp("app-with-flows");

    HttpRequest request =
        HttpRequest.builder().setUri("http://localhost:" + httpPort.getNumber() + "/nonBlocking").setMethod(GET).build();
    httpClient.send(request, MESSAGE_TIMEOUT, false, null);

    muleContextForApp.dispose();

    assertEventsUnreferenced();
  }

  @Test
  public void httpRequest() throws IOException, TimeoutException {
    MuleContext muleContextForApp = getMuleContextForApp("app-with-flows");

    HttpRequest request =
        HttpRequest.builder().setUri("http://localhost:" + httpPort.getNumber() + "/request").setMethod(GET).build();
    httpClient.send(request, MESSAGE_TIMEOUT, false, null);
    muleContextForApp.dispose();

    assertEventsUnreferenced();
  }

  @Test
  @Ignore("Reimplement with the new JMS Connector")
  public void jms() throws MuleException {
    final MuleContext muleContextForApp = getMuleContextForApp("app-with-flows");

    muleContextForApp.getClient().dispatch("jms://in?connector=sharedJmsConnector", of("payload"));
    muleContextForApp.getClient().request("jms://out?connector=sharedJmsConnector", MESSAGE_TIMEOUT);

    muleContextForApp.dispose();

    assertEventsUnreferenced();
  }

  private void assertEventsUnreferenced() {
    new PollingProber(PROBER_POLIING_TIMEOUT, PROBER_POLLING_INTERVAL).check(new JUnitProbe() {

      @Override
      protected boolean test() throws Exception {
        System.gc();
        for (PhantomReference<Event> phantomReference : requestContextRefs) {
          assertThat(phantomReference.isEnqueued(), is(true));
        }
        return true;
      }
    });
  }
}

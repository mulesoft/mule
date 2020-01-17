/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.reconnection;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.api.util.ClassUtils.getFieldValue;
import static org.mule.tck.probe.PollingProber.check;

import org.mule.extension.test.extension.reconnection.ReconnectableConnection;
import org.mule.extension.test.extension.reconnection.ReconnectableConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.streaming.object.CursorIterator;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.retry.policy.RetryPolicy;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import java.time.Duration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

public class ReconnectionTestCase extends AbstractExtensionFunctionalTestCase {

  private static List<CoreEvent> capturedEvents;

  public static class CaptureProcessor implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      synchronized (capturedEvents) {
        capturedEvents.add(event);
      }
      return event;
    }
  }

  @Override
  protected String getConfigFile() {
    return "reconnection-config.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    capturedEvents = new LinkedList<>();
    ReconnectableConnectionProvider.fail = false;
  }

  @Override
  protected void doTearDown() throws Exception {
    capturedEvents = null;
    ReconnectableConnectionProvider.fail = false;
  }

  @Test
  public void reconnectSource() throws Exception {
    ((Startable) getFlowConstruct("reconnectForever")).start();
    check(5000, 1000, () -> !capturedEvents.isEmpty());
    switchConnection();

    check(10000, 1000, () -> {
      synchronized (capturedEvents) {
        return capturedEvents.stream()
            .map(event -> (ReconnectableConnection) event.getMessage().getPayload().getValue())
            .filter(c -> c.getReconnectionAttempts() >= 3)
            .findAny()
            .isPresent();
      }
    });
  }

  @Test
  public void connectionIsClosedDuringConnectionExceptionOnFirstPage() throws Exception {
    Iterator<ReconnectableConnection> iterator = getCursor("pagedOperation", 1);
    ReconnectableConnection firstPage = iterator.next();
    assertThat(firstPage.getDisconnectCalls(), is(1));
    assertThat(firstPage.getClosePagingProviderCalls(), is(1));
    assertThat(firstPage.getDisconnectCalls(), is(1));
    assertThat(firstPage.getDisconnectCalls(), is(1));
  }

  @Test
  public void connectionIsClosedDuringConnectionExceptionOnSecondPage() throws Exception {
    Iterator<ReconnectableConnection> iterator = getCursor("pagedOperation", 2);

    ReconnectableConnection firstPage = iterator.next();
    assertThat(firstPage.getDisconnectCalls(), is(0));
    assertThat(firstPage.getClosePagingProviderCalls(), is(0));

    ReconnectableConnection secondPage = iterator.next();
    assertThat(secondPage.getDisconnectCalls(), is(1));
    assertThat(secondPage.getClosePagingProviderCalls(), is(1));
  }

  @Test
  public void getRetryPolicyTemplateFromConfig() throws Exception {
    RetryPolicyTemplate template = (RetryPolicyTemplate) flowRunner("getReconnectionFromConfig").run()
        .getMessage().getPayload().getValue();

    assertRetryTemplate(template, false, 3, 1000);
  }

  @Test
  public void getInlineRetryPolicyTemplate() throws Exception {
    RetryPolicyTemplate template = (RetryPolicyTemplate) flowRunner("getInlineReconnection").run()
        .getMessage().getPayload().getValue();

    assertRetryTemplate(template, false, 30, 50);
  }

  private void assertRetryTemplate(RetryPolicyTemplate template, boolean async, int count, long freq) throws Exception {
    assertThat(template.isAsync(), is(async));

    RetryPolicy policy = template.createRetryInstance();

    assertThat(getFieldValue(policy, "count", false), is(count));
    Duration duration = getFieldValue(policy, "frequency", false);
    assertThat(duration.toMillis(), is(freq));
  }

  private void switchConnection() throws Exception {
    flowRunner("switchConnection").run();
  }

  private <T> CursorIterator<T> getCursor(String flowName, Integer failOn) throws Exception {
    CursorIteratorProvider provider =
        (CursorIteratorProvider) flowRunner(flowName).withPayload(failOn).keepStreamsOpen().run().getMessage().getPayload()
            .getValue();

    return provider.openCursor();
  }
}

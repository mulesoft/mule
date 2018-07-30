/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.context.thread.notification;

import static org.mockito.Mockito.*;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_ACTIVATE_SCHEDULERS_LATENCY_REPORT;
import static reactor.core.publisher.Flux.just;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

public class ThreadNotificationLoggerTestCase {

  @ClassRule
  public static SystemProperty logging = new SystemProperty(MULE_ACTIVATE_SCHEDULERS_LATENCY_REPORT, "true");

  private ThreadNotificationLogger logger;
  private ThreadNotificationService service = mock(ThreadNotificationService.class);
  private CoreEvent event = mock(CoreEvent.class);
  private EventContext context = mock(EventContext.class);

  @Before
  public void setup() {
    logger = new ThreadNotificationLogger(service);
    when(event.getContext()).thenReturn(context);
    when(context.getId()).thenReturn("id");
  }

  @Test
  public void withoutThreadSwitch() {
    Reference<Boolean> checked = new Reference<>(false);
    just(event)
        .doOnNext(coreEvent -> logger.setStartingThread(event))
        .map(coreEvent -> coreEvent)
        .doOnNext(coreEvent -> logger.setFinishThread(event))
        .doOnNext(coreEvent -> {
          verify(service, never()).addThreadNotificationElement(any());
          checked.set(true);
        }).subscribe();

    new PollingProber().check(new JUnitLambdaProbe(() -> checked.get()));
  }



}

/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.processor.chain;

import static org.mule.runtime.core.api.alert.MuleAlertingSupport.AlertNames.ALERT_REACTOR_DISCARDED_EVENT;
import static org.mule.runtime.core.api.alert.MuleAlertingSupport.AlertNames.ALERT_REACTOR_DROPPED_ERROR;
import static org.mule.runtime.core.api.alert.MuleAlertingSupport.AlertNames.ALERT_REACTOR_DROPPED_EVENT;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.tck.probe.PollingProber.probe;
import static org.mule.test.allure.AllureConstants.SupportabilityFeature.SUPPORTABILITY;
import static org.mule.test.allure.AllureConstants.SupportabilityFeature.SupportabilityStory.ALERTS;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Operators.emptySubscription;

import static org.junit.Assert.fail;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.mule.runtime.api.alert.AlertingSupport;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.tck.junit4.AbstractReactiveProcessorTestCase;

import java.util.concurrent.ExecutorService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(SUPPORTABILITY)
@Story(ALERTS)
public class ReactorChainsAlertsTestCase extends AbstractReactiveProcessorTestCase {

  private static final Logger LOGGER = getLogger(DefaultMessageProcessorChainTestCase.class);

  private AlertingSupport alertingSupport;

  private ExecutorService asyncExecutor;

  private MessageProcessorChain messageProcessor;

  private CoreEvent event;

  public ReactorChainsAlertsTestCase(Mode mode) {
    super(mode);
  }

  @Before
  public void before() throws MuleException {
    event = testEvent();
    alertingSupport = mock(AlertingSupport.class);

    asyncExecutor = newSingleThreadExecutor();
  }

  @After
  public void after() throws MuleException {
    if (messageProcessor != null) {
      stopIfNeeded(messageProcessor);
      disposeIfNeeded(messageProcessor, LOGGER);

      messageProcessor = null;
    }

    asyncExecutor.shutdownNow();
  }

  @Test
  public void discardingProcessor() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    var mp1 = new EventDiscardingProcessor();
    builder.chain(mp1);
    messageProcessor = builder.build();

    muleContext.getInjector().inject(messageProcessor);
    ((AbstractMessageProcessorChain) messageProcessor).setAlertingSupport(alertingSupport);
    initialiseIfNeeded(messageProcessor);
    startIfNeeded(messageProcessor);

    asyncExecutor.execute(() -> {
      try {
        process(messageProcessor, event);
      } catch (Exception e) {
        fail(e.toString());
      }
    });

    probe(() -> {
      verify(alertingSupport).triggerAlert(eq(ALERT_REACTOR_DISCARDED_EVENT), any());

      verify(alertingSupport, never()).triggerAlert(ALERT_REACTOR_DROPPED_EVENT);
      verify(alertingSupport, never()).triggerAlert(eq(ALERT_REACTOR_DROPPED_EVENT), any());
      verify(alertingSupport, never()).triggerAlert(ALERT_REACTOR_DROPPED_ERROR);
      verify(alertingSupport, never()).triggerAlert(eq(ALERT_REACTOR_DROPPED_ERROR), any());

      return true;
    });
  }

  @Test
  public void droppingProcessor() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    var mp1 = new EventDroppingProcessor();
    builder.chain(mp1);
    messageProcessor = builder.build();

    muleContext.getInjector().inject(messageProcessor);
    ((AbstractMessageProcessorChain) messageProcessor).setAlertingSupport(alertingSupport);
    initialiseIfNeeded(messageProcessor);
    startIfNeeded(messageProcessor);

    asyncExecutor.execute(() -> {
      try {
        process(messageProcessor, event);
      } catch (Exception e) {
        fail(e.toString());
      }
    });

    probe(() -> {
      verify(alertingSupport).triggerAlert(eq(ALERT_REACTOR_DROPPED_EVENT), any());

      verify(alertingSupport, never()).triggerAlert(ALERT_REACTOR_DISCARDED_EVENT);
      verify(alertingSupport, never()).triggerAlert(eq(ALERT_REACTOR_DISCARDED_EVENT), any());
      verify(alertingSupport, never()).triggerAlert(ALERT_REACTOR_DROPPED_ERROR);
      verify(alertingSupport, never()).triggerAlert(eq(ALERT_REACTOR_DROPPED_ERROR), any());

      return true;
    });
  }

  @Test
  public void errorDroppingProcessor() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    var mp1 = new ErrorDroppingProcessor();
    builder.chain(mp1);
    messageProcessor = builder.build();

    muleContext.getInjector().inject(messageProcessor);
    ((AbstractMessageProcessorChain) messageProcessor).setAlertingSupport(alertingSupport);
    initialiseIfNeeded(messageProcessor);
    startIfNeeded(messageProcessor);

    asyncExecutor.execute(() -> {
      try {
        process(messageProcessor, event);
      } catch (Exception e) {
        fail(e.toString());
      }
    });

    probe(() -> {
      verify(alertingSupport).triggerAlert(eq(ALERT_REACTOR_DROPPED_ERROR), any());

      verify(alertingSupport, never()).triggerAlert(ALERT_REACTOR_DISCARDED_EVENT);
      verify(alertingSupport, never()).triggerAlert(eq(ALERT_REACTOR_DISCARDED_EVENT), any());
      verify(alertingSupport, never()).triggerAlert(ALERT_REACTOR_DROPPED_EVENT);
      verify(alertingSupport, never()).triggerAlert(eq(ALERT_REACTOR_DROPPED_EVENT), any());

      return true;
    });
  }

  private static class EventDiscardingProcessor implements Processor {

    @Override
    public Publisher<CoreEvent> apply(Publisher<CoreEvent> p) {
      return from(p).filter(event -> false);
    }

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      throw new UnsupportedOperationException();
    }

  }

  private static class EventDroppingProcessor implements Processor {

    @Override
    public Publisher<CoreEvent> apply(Publisher<CoreEvent> p) {
      return from(p)
          .doOnNext(e -> from((Publisher<Integer>) s -> {
            s.onSubscribe(emptySubscription());
            s.onNext(1);
          })
              .take(0, false)
              .subscribe());
    }

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      throw new UnsupportedOperationException();
    }

  }

  private static class ErrorDroppingProcessor implements Processor {

    @Override
    public Publisher<CoreEvent> apply(Publisher<CoreEvent> p) {
      return from(p)
          .doOnNext(e -> from((Publisher<Integer>) s -> {
            s.onSubscribe(emptySubscription());
            s.onError(new RuntimeException("Force error drop"));
          })
              .take(0, false)
              .subscribe());
    }

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      throw new UnsupportedOperationException();
    }

  }

}

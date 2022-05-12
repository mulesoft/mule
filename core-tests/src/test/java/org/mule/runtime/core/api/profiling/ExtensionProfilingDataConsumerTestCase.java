/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.profiling;

import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.EXTENSION_PROFILING_EVENT;
import static org.mule.runtime.api.util.MuleSystemProperties.ENABLE_PROFILING_SERVICE_PROPERTY;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_PROFILING_SERVICE;

import static com.google.common.collect.ImmutableSet.of;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.profiling.ProfilingDataConsumer;
import org.mule.runtime.api.profiling.ProfilingDataConsumerDiscoveryStrategy;
import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.api.profiling.type.context.ExtensionProfilingEventContext;
import org.mule.runtime.core.internal.processor.strategy.util.ProfilingUtils;
import org.mule.runtime.core.internal.profiling.ArtifactProfilingProducerScope;
import org.mule.runtime.core.internal.profiling.DefaultProfilingService;
import org.mule.runtime.core.internal.profiling.consumer.annotations.RuntimeInternalProfilingDataConsumer;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mule.tck.junit4.rule.SystemProperty;
import org.slf4j.Logger;

@Feature(PROFILING)
@Story(DEFAULT_PROFILING_SERVICE)
public class ExtensionProfilingDataConsumerTestCase extends AbstractMuleContextTestCase {

  public static final String STATUS = "STATUS";
  public static final String OK = "OK";
  public static final String PROFILING_EVENT_CONTEXT = "PROFILING_EVENT_CONTEXT";
  public static final String COMPONENT_PROFILING_EVENT_IDENTIFIER = "COMPONENT_PROFILING_EVENT_IDENTIFIER";
  public static final String TEST_DATA_CONSUMER = "TEST_DATA_CONSUMER";

  @Rule
  public MockitoRule mockitorule = MockitoJUnit.rule();

  @Rule
  public SystemProperty enableProfilingServiceProperty = new SystemProperty(ENABLE_PROFILING_SERVICE_PROPERTY, "true");

  @Rule
  public EnableInternalRuntimeProfilers enableInternalRuntimeProfilfers =
      new EnableInternalRuntimeProfilers(new TestComponentProfilingDataConsumer(null));

  @Mock
  private ExtensionProfilingEventContext profilingEventContext;

  @Mock
  private Logger logger;

  private TestDefaultProfilingService profilingService;

  @Before
  public void before() throws Exception {
    when(profilingEventContext.getProfilingDataSourceIdentifier()).thenReturn(PROFILING_EVENT_CONTEXT);
    when(profilingEventContext.get(STATUS)).thenReturn(Optional.of("OK"));
    when(profilingEventContext.getExtensionEventSubtypeIdentifier()).thenReturn(COMPONENT_PROFILING_EVENT_IDENTIFIER);
    profilingService = new TestDefaultProfilingService(logger);
    initialiseIfNeeded(profilingService, muleContext);
    startIfNeeded(profilingService);
  }



  @Test
  @Description("When a profiling event related to an extension is triggered, the data consumers process the data accordingly.")
  public void dataConsumersForComponentProfilingEventAreTriggered() throws Exception {
    doTestLoggerWhenProfilingDataProducerIsTriggered(logger);
  }

  @Test
  @Description("When an extension profiler data consumer is dynamically registered in a connector, it consumes the messages")
  public void dataConsumersDynamicallyRegistered() {
    Logger loggerForDynamicDataConsumer = mock(Logger.class);
    profilingService.registerProfilingDataConsumer(new TestComponentProfilingDataConsumer(loggerForDynamicDataConsumer));
    doTestLoggerWhenProfilingDataProducerIsTriggered(loggerForDynamicDataConsumer);
  }

  private void doTestLoggerWhenProfilingDataProducerIsTriggered(Logger logger) {
    ProfilingDataProducer<ExtensionProfilingEventContext, Object> dataProducer =
        profilingService.getProfilingDataProducer(EXTENSION_PROFILING_EVENT,
                                                  new ArtifactProfilingProducerScope(ProfilingUtils.getArtifactId(muleContext)));
    dataProducer.triggerProfilingEvent(new Object(), o -> profilingEventContext);

    verify(logger).info(PROFILING_EVENT_CONTEXT);
    verify(logger).info(OK);
    verify(logger).info(COMPONENT_PROFILING_EVENT_IDENTIFIER);
  }

  /**
   * Stub {@link DefaultProfilingService} with a test {@link ProfilingDataConsumerDiscoveryStrategy}.
   */
  private static class TestDefaultProfilingService extends DefaultProfilingService {

    private final Logger logger;

    public TestDefaultProfilingService(Logger logger) {
      this.logger = logger;
    }

    @Override
    public ProfilingDataConsumerDiscoveryStrategy getDiscoveryStrategy() {
      return new TestProfilingDataConsumerDiscoveryStrategy(logger);
    }

  }


  /**
   * Stub {@link ProfilingDataConsumerDiscoveryStrategy}
   */
  private static class TestProfilingDataConsumerDiscoveryStrategy implements ProfilingDataConsumerDiscoveryStrategy {

    private final Logger logger;

    public TestProfilingDataConsumerDiscoveryStrategy(Logger logger) {
      this.logger = logger;
    }

    @Override
    public Set<ProfilingDataConsumer<?>> discover() {
      return of(new TestComponentProfilingDataConsumer(logger));
    }

  }

  /**
   * Stub {@link ProfilingDataConsumer} for consuming component profiling events.
   */
  @RuntimeInternalProfilingDataConsumer
  private static class TestComponentProfilingDataConsumer implements ProfilingDataConsumer<ExtensionProfilingEventContext> {

    private final Logger logger;

    public TestComponentProfilingDataConsumer(Logger logger) {
      this.logger = logger;
    }

    @Override
    public void onProfilingEvent(ProfilingEventType<ExtensionProfilingEventContext> profilingEventType,
                                 ExtensionProfilingEventContext profilingEventContext) {
      logger.info(profilingEventContext.getProfilingDataSourceIdentifier());
      logger.info(profilingEventContext.getExtensionEventSubtypeIdentifier());
      logger.info((String) profilingEventContext.get(STATUS).orElse(null));
    }

    @Override
    public Set<ProfilingEventType<ExtensionProfilingEventContext>> getProfilingEventTypes() {
      return of(EXTENSION_PROFILING_EVENT);
    }

    @Override
    public Predicate<ExtensionProfilingEventContext> getEventContextFilter() {
      return eventContext -> true;
    }
  }


}

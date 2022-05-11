/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.profiling;

import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.MEMORY_BYTE_BUFFER_ALLOCATION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.MEMORY_BYTE_BUFFER_DEALLOCATION;
import static org.mule.runtime.api.util.MuleSystemProperties.ENABLE_PROFILING_SERVICE_PROPERTY;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.internal.profiling.consumer.ComponentProfilingUtils.getByteBufferProfilingInfo;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_PROFILING_SERVICE;

import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;

import static com.google.common.collect.ImmutableSet.of;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.profiling.ProfilingDataConsumer;
import org.mule.runtime.api.profiling.ProfilingDataConsumerDiscoveryStrategy;
import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.ProfilingService;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.api.profiling.type.context.ByteBufferProviderEventContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.profiling.DefaultProfilingService;
import org.mule.runtime.core.internal.profiling.consumer.LoggerByteBufferAllocationProfilingDataConsumer;
import org.mule.runtime.core.internal.profiling.consumer.annotations.RuntimeInternalProfilingDataConsumer;
import org.mule.runtime.internal.memory.bytebuffer.profiling.DefaultByteBufferProviderEventContext;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.Collection;
import java.util.Set;

import com.google.gson.Gson;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.slf4j.Logger;

@Feature(PROFILING)
@Story(DEFAULT_PROFILING_SERVICE)
@RunWith(Parameterized.class)
public class LoggerByteBufferAllocationProfilingDataConsumerTestCase extends AbstractMuleContextTestCase {

  public static final String ARTIFACT_ID = "artifactId";
  public static final String TEST_BYTE_BUFFER_PROVIDER = "TEST_BYTE_BUFFER_PROVIDER";

  @Rule
  public MockitoRule mockitorule = MockitoJUnit.rule();

  @Rule
  public SystemProperty enableProfilingServiceProperty = new SystemProperty(ENABLE_PROFILING_SERVICE_PROPERTY, "true");

  @Rule
  public EnableInternalRuntimeProfilers enableInternalRuntimeProfilers =
      new EnableInternalRuntimeProfilers(new TestLoggerByteBufferAllocationProfilingDataConsumer(null));

  @Mock
  private ComponentLocation location;

  @Mock
  private Logger logger;

  @Mock
  private TypedComponentIdentifier componentIdentifier;

  @Mock
  private ComponentIdentifier identifier;

  private final Gson gson = new Gson();

  private final ProfilingEventType<ByteBufferProviderEventContext> profilingEventType;

  private ProfilingService profilingService;

  @Before
  public void before() throws Exception {
    when(logger.isDebugEnabled()).thenReturn(true);
    when(location.getComponentIdentifier()).thenReturn(componentIdentifier);
    when(componentIdentifier.getIdentifier()).thenReturn(identifier);
    when(identifier.getName()).thenReturn("test");
    when(identifier.getNamespace()).thenReturn("test");
    profilingService = getTestProfilingService();
  }

  private ProfilingService getTestProfilingService() throws MuleException {
    ProfilingService profilingService = new TestDefaultProfilingService(logger);
    initialiseIfNeeded(profilingService, muleContext);
    startIfNeeded(profilingService);
    return profilingService;
  }

  @Parameters(name = "eventType: {0}")
  public static Collection<ProfilingEventType<ByteBufferProviderEventContext>> eventType() {
    return asList(MEMORY_BYTE_BUFFER_ALLOCATION, MEMORY_BYTE_BUFFER_DEALLOCATION);
  }

  public LoggerByteBufferAllocationProfilingDataConsumerTestCase(ProfilingEventType<ByteBufferProviderEventContext> profilingEventType) {
    this.profilingEventType = profilingEventType;
  }

  @Test
  @Description("When a profiling event related to memory is triggered, the data consumers process the data accordingly.")
  public void dataConsumersForThreadingProfilingEventTypesConsumeDataAccordingly() {
    ProfilingDataProducer<ByteBufferProviderEventContext, Object> dataProducer =
        profilingService.getProfilingDataProducer(profilingEventType);

    ByteBufferProviderEventContext profilerEventContext =
        new DefaultByteBufferProviderEventContext(TEST_BYTE_BUFFER_PROVIDER, currentTimeMillis(), 50);
    dataProducer.triggerProfilingEvent(profilerEventContext);

    verify(logger).debug(gson.toJson(getByteBufferProfilingInfo(profilingEventType, profilerEventContext)));
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
      return of(new TestLoggerByteBufferAllocationProfilingDataConsumer(logger));
    }
  }

  /**
   * Stub {@link LoggerByteBufferAllocationProfilingDataConsumer} for injecting a mocked {@link Logger}
   */
  @RuntimeInternalProfilingDataConsumer
  private static class TestLoggerByteBufferAllocationProfilingDataConsumer
      extends LoggerByteBufferAllocationProfilingDataConsumer {

    private final Logger logger;

    public TestLoggerByteBufferAllocationProfilingDataConsumer(Logger logger) {
      super();
      this.logger = logger;
    }

    @Override
    protected Logger getDataConsumerLogger() {
      return logger;
    }
  }

}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.profiling;

import static com.google.common.collect.ImmutableSet.of;
import static java.util.Arrays.asList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.OPERATION_EXECUTED;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.OPERATION_THREAD_RELEASE;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.STARTING_OPERATION_EXECUTION;
import static org.mule.runtime.api.util.MuleSystemProperties.ENABLE_PROFILING_SERVICE_PROPERTY;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.internal.config.FeatureFlaggingUtils.setFeatureState;
import static org.mule.runtime.core.internal.config.FeatureFlaggingUtils.withFeatureUser;
import static org.mule.runtime.core.internal.processor.strategy.util.ProfilingUtils.getArtifactId;
import static org.mule.runtime.core.internal.profiling.consumer.ComponentProfilingUtils.getComponentThreadingInfoMap;
import static org.mule.runtime.core.internal.config.togglz.MuleTogglzFeatureManagerProvider.FEATURE_PROVIDER;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_PROFILING_SERVICE;

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
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.profiling.ProfilingDataConsumer;
import org.mule.runtime.api.profiling.ProfilingDataConsumerDiscoveryStrategy;
import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.ProfilingService;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.api.profiling.type.context.ComponentThreadingProfilingEventContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.config.togglz.user.MuleTogglzArtifactFeatureUser;
import org.mule.runtime.core.internal.profiling.DefaultProfilingService;
import org.mule.runtime.core.internal.profiling.consumer.LoggerComponentProcessingStrategyDataConsumer;
import org.mule.runtime.core.internal.profiling.consumer.LoggerComponentThreadingDataConsumer;
import org.mule.runtime.core.internal.profiling.context.DefaultComponentThreadingProfilingEventContext;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
import org.slf4j.Logger;
import org.togglz.core.repository.FeatureState;

import java.util.Collection;
import java.util.Set;

@Feature(PROFILING)
@Story(DEFAULT_PROFILING_SERVICE)
@RunWith(Parameterized.class)
public class ThreadingDataConsumersTestCase extends AbstractMuleContextTestCase {

  public static final String THREAD_NAME = "threadName";
  public static final String ARTIFACT_ID = "artifactId";
  public static final String ARTIFACT_TYPE = "artifactType";
  public static final long PROFILING_EVENT_TIMESTAMP = 5678L;

  @Rule
  public MockitoRule mockitorule = MockitoJUnit.rule();

  @Rule
  public SystemProperty systemProperty = new SystemProperty(ENABLE_PROFILING_SERVICE_PROPERTY, "true");

  @Mock
  private CoreEvent event;

  @Mock
  private ComponentLocation location;

  @Mock
  private Logger logger;

  @Mock
  private TypedComponentIdentifier componentIdentifier;

  @Mock
  private ComponentIdentifier identifier;

  private final Gson gson = new Gson();

  private final ProfilingEventType<ComponentThreadingProfilingEventContext> profilingEventType;

  private ProfilingService profilingService;

  @Before
  public void before() throws Exception {
    when(logger.isDebugEnabled()).thenReturn(true);
    when(location.getComponentIdentifier()).thenReturn(componentIdentifier);
    when(componentIdentifier.getIdentifier()).thenReturn(identifier);
    when(identifier.getName()).thenReturn("test");
    when(identifier.getNamespace()).thenReturn("test");
    profilingService = getTestProfilingService();
    enableProfilingFeatures();
  }

  private void enableProfilingFeatures() {
    withFeatureUser(new MuleTogglzArtifactFeatureUser(getArtifactId(muleContext)), () -> {
      setFeatureState(new FeatureState(FEATURE_PROVIDER.getOrRegisterProfilingTogglzFeatureFrom(STARTING_OPERATION_EXECUTION,
                                                                                                "TEST_CONSUMER"),
                                       true));
    });

    withFeatureUser(new MuleTogglzArtifactFeatureUser(getArtifactId(muleContext)), () -> {
      setFeatureState(new FeatureState(FEATURE_PROVIDER.getOrRegisterProfilingTogglzFeatureFrom(OPERATION_EXECUTED,
                                                                                                "TEST_CONSUMER"),
                                       true));
    });

    withFeatureUser(new MuleTogglzArtifactFeatureUser(getArtifactId(muleContext)), () -> {
      setFeatureState(new FeatureState(FEATURE_PROVIDER.getOrRegisterProfilingTogglzFeatureFrom(OPERATION_THREAD_RELEASE,
                                                                                                "TEST_CONSUMER"),
                                       true));
    });
  }

  private ProfilingService getTestProfilingService() throws MuleException {
    ProfilingService profilingService = new TestDefaultProfilingService(logger);
    initialiseIfNeeded(profilingService, muleContext);
    startIfNeeded(profilingService);
    return profilingService;
  }

  @Parameters(name = "eventType: {0}")
  public static Collection<ProfilingEventType<ComponentThreadingProfilingEventContext>> eventType() {
    return asList(STARTING_OPERATION_EXECUTION, OPERATION_EXECUTED, OPERATION_THREAD_RELEASE);
  }

  public ThreadingDataConsumersTestCase(ProfilingEventType<ComponentThreadingProfilingEventContext> profilingEventType) {
    this.profilingEventType = profilingEventType;
  }

  @Test
  @Description("When a profiling event related to threading is triggered, the data consumers process the data accordingly.")
  public void dataConsumersForThreadingProfilingEventTypesConsumeDataAccordingly() {
    ProfilingDataProducer<ComponentThreadingProfilingEventContext, Object> dataProducer =
        profilingService.getProfilingDataProducer(profilingEventType);

    ComponentThreadingProfilingEventContext profilerEventContext =
        new DefaultComponentThreadingProfilingEventContext(event, location, THREAD_NAME, ARTIFACT_ID, ARTIFACT_TYPE,
                                                           PROFILING_EVENT_TIMESTAMP);
    dataProducer.triggerProfilingEvent(profilerEventContext);

    verify(logger).debug(jsonToLog(profilingEventType, profilerEventContext));
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
      return of(new TestLoggerComponentThreadingDataConsumer(logger));
    }
  }

  /**
   * Stub {@link LoggerComponentProcessingStrategyDataConsumer} for injecting a mocked {@link Logger}
   */
  private static class TestLoggerComponentThreadingDataConsumer extends LoggerComponentThreadingDataConsumer {

    private final Logger logger;

    public TestLoggerComponentThreadingDataConsumer(Logger logger) {
      super();
      this.logger = logger;
    }

    @Override
    protected Logger getDataConsumerLogger() {
      return logger;
    }
  }

  private String jsonToLog(ProfilingEventType<ComponentThreadingProfilingEventContext> profilingEventType,
                           ComponentThreadingProfilingEventContext profilingEventContext) {
    return gson.toJson(getComponentThreadingInfoMap(profilingEventType, profilingEventContext));
  }

}

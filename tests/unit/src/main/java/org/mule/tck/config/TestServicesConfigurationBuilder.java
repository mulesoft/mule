/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.config;

import static java.lang.Thread.currentThread;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.api.scheduler.SchedulerConfig.config;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_SCHEDULER_BASE_CONFIG;

import org.mule.runtime.api.el.DefaultExpressionLanguageFactoryService;
import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.api.el.ExpressionLanguageConfiguration;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.scheduler.SchedulerView;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.runtime.http.api.HttpService;
import org.mule.tck.SimpleUnitTestSupportSchedulerService;
import org.mule.weave.v2.el.WeaveDefaultExpressionLanguageFactoryService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Registers services instances into the {@link MuleRegistry} of a {@link MuleContext}.
 * <p>
 * This is to be used only in tests that do not leverage the service injection mechanism.
 *
 * @since 4.0
 */
public class TestServicesConfigurationBuilder extends AbstractConfigurationBuilder implements TestRule {

  private static final String MOCK_HTTP_SERVICE = "mockHttpService";
  private static final String MOCK_EXPR_EXECUTOR = "mockExpressionExecutor";

  private static DefaultExpressionLanguageFactoryService cachedExprLanguageFactory;
  private static int cachedExprLanguageFactoryCounter = 0;

  private final SimpleUnitTestSupportSchedulerService schedulerService = new SimpleUnitTestSupportSchedulerService();

  private final boolean mockHttpService;
  private final boolean mockExpressionExecutor;

  private final Map<String, Object> additionalMockedServices = new HashMap<>();

  private final Map<String, Object> overriddenDefaultServices = new HashMap<>();

  public TestServicesConfigurationBuilder() {
    this(true, true);
  }

  public TestServicesConfigurationBuilder(boolean mockHttpService, boolean mockExpressionExecutor) {
    this.mockHttpService = mockHttpService;
    this.mockExpressionExecutor = mockExpressionExecutor;
  }

  @Override
  public void doConfigure(MuleContext muleContext) {
    Thread currentThread = currentThread();
    ClassLoader originalClassLoader = currentThread.getContextClassLoader();
    currentThread.setContextClassLoader(TestServicesConfigurationBuilder.class.getClassLoader());
    try {
      MuleRegistry registry = ((MuleContextWithRegistry) muleContext).getRegistry();
      registry.registerObject(schedulerService.getName(), spy(schedulerService));
      registry.registerObject(OBJECT_SCHEDULER_BASE_CONFIG, config());

      if (mockExpressionExecutor) {
        DefaultExpressionLanguageFactoryService expressionExecutor =
            mock(DefaultExpressionLanguageFactoryService.class, RETURNS_DEEP_STUBS);
        registry.registerObject(MOCK_EXPR_EXECUTOR, expressionExecutor);
      } else {
        // Avoid doing the DW warm-up for every test, reusing the ExpressionLanguage implementation
        // Still have to recreate ever once in a while so global bindings added for each test are accumulated.
        if (cachedExprLanguageFactory == null || cachedExprLanguageFactoryCounter > 20) {
          cachedExprLanguageFactoryCounter = 0;
          final DefaultExpressionLanguageFactoryService exprExecutor = new WeaveDefaultExpressionLanguageFactoryService(null);
          ExpressionLanguage exprLanguage = exprExecutor.create();
          // Force initialization of internal DataWeave stuff
          // This way we avoid doing some heavy initialization on the test itself,
          // which may cause trouble when evaluation expressions in places with small timeouts
          exprLanguage.evaluate("{dataWeave: 'is'} ++ {mule: 'default EL'}", NULL_BINDING_CONTEXT);

          cachedExprLanguageFactory = new DefaultExpressionLanguageFactoryService() {

            @Override
            public ExpressionLanguage create() {
              return exprLanguage;
            }

            @Override
            public ExpressionLanguage create(ExpressionLanguageConfiguration configuration) {
              return exprLanguage;
            }

            @Override
            public String getName() {
              return exprExecutor.getName();
            }
          };
        } else {
          cachedExprLanguageFactoryCounter++;
        }

        registry.registerObject(cachedExprLanguageFactory.getName(), cachedExprLanguageFactory);
      }

      if (mockHttpService) {
        registry.registerObject(MOCK_HTTP_SERVICE, mock(HttpService.class));
      }

      overriddenDefaultServices.forEach((serviceId, serviceImpl) -> (muleContext).getCustomizationService()
          .overrideDefaultServiceImpl(serviceId, serviceImpl));

      registry.registerObjects(additionalMockedServices);
    } catch (RegistrationException e) {
      throw new MuleRuntimeException(e);
    } finally {
      currentThread.setContextClassLoader(originalClassLoader);
    }
  }

  public void stopServices() throws MuleException {
    final List<SchedulerView> schedulers = schedulerService.getSchedulers();
    try {
      assertThat(schedulers, empty());
    } finally {
      schedulerService.stop();
    }
  }

  public void registerAdditionalService(String name, Object service) {
    this.additionalMockedServices.put(name, service);
  }

  public void registerOverriddenService(String name, Object service) {
    this.overriddenDefaultServices.put(name, service);
  }

  @Override
  public Statement apply(Statement base, Description description) {
    return new Statement() {

      @Override
      public void evaluate() throws Throwable {
        try {
          base.evaluate();
        } finally {
          stopServices();
        }
      }
    };
  }

}

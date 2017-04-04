/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.interception;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.el.MuleExpressionLanguage;
import org.mule.runtime.api.interception.InterceptionEvent;
import org.mule.runtime.api.interception.ProcessorInterceptor;
import org.mule.runtime.api.interception.ProcessorInterceptorFactory;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.service.http.api.HttpService;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;

public class ProcessorInterceptorFactoryTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/interception/processor-interceptor-factory.xml";
  }

  @Override
  protected void addBuilders(List<ConfigurationBuilder> builders) {
    super.addBuilders(builders);

    builders.add(new ConfigurationBuilder() {

      @Override
      public boolean isConfigured() {
        return false;
      }

      @Override
      public void configure(MuleContext muleContext) throws ConfigurationException {
        muleContext.getProcessorInterceptorManager().addInterceptorFactory(new HasInjectedAttributesInterceptorFactory());
      }
    });
  }

  @Before
  public void before() {
    HasInjectedAttributesInterceptor.intercepted.set(false);
  }

  @Test
  public void injection() throws Exception {
    flowRunner("injectionInterceptionTest").run();
    assertThat(HasInjectedAttributesInterceptor.intercepted.get(), is(true));
  }

  public static class HasInjectedAttributesInterceptorFactory implements ProcessorInterceptorFactory {

    @Inject
    private MuleExpressionLanguage expressionEvaluator;

    @Inject
    private LockFactory lockFactory;

    @Inject
    private HttpService httpService;

    @Override
    public ProcessorInterceptor get() {
      return new HasInjectedAttributesInterceptor(expressionEvaluator, lockFactory, httpService);
    }
  }

  public static class HasInjectedAttributesInterceptor implements ProcessorInterceptor {

    public static final AtomicBoolean intercepted = new AtomicBoolean(false);

    private MuleExpressionLanguage expressionEvaluator;
    private LockFactory lockFactory;
    private HttpService httpService;

    public HasInjectedAttributesInterceptor(MuleExpressionLanguage expressionEvaluator, LockFactory lockFactory,
                                            HttpService httpService) {
      this.expressionEvaluator = expressionEvaluator;
      this.lockFactory = lockFactory;
      this.httpService = httpService;
    }

    @Override
    public void before(Map<String, Object> parameters, InterceptionEvent event) {
      intercepted.set(true);
      assertThat(expressionEvaluator, not(nullValue()));
      assertThat(lockFactory, not(nullValue()));
      assertThat(httpService, not(nullValue()));
    }
  }

}

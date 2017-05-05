/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.interception;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.test.allure.AllureConstants.InterceptonApi.INTERCEPTION_API;
import static org.mule.test.allure.AllureConstants.InterceptonApi.ComponentInterceptionStory.COMPONENT_INTERCEPTION_STORY;

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
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.heisenberg.extension.model.KillParameters;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features(INTERCEPTION_API)
@Stories(COMPONENT_INTERCEPTION_STORY)
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
    HasInjectedAttributesInterceptor.interceptionParameters.clear();
  }

  @Description("Logger, flow-ref and splitter components are intercepted in order and the parameters are correctly sent")
  @Test
  public void injection() throws Exception {
    flowRunner("injectionInterceptionTest").run();
    assertThat(HasInjectedAttributesInterceptor.interceptionParameters.size(), is(3));
    List<InterceptionParameters> interceptionParameters = HasInjectedAttributesInterceptor.interceptionParameters;

    InterceptionParameters loggerInterceptionParameter = interceptionParameters.get(0);
    InterceptionParameters flowRefInterceptionParameter = interceptionParameters.get(1);
    InterceptionParameters splitterInterceptionParameter = interceptionParameters.get(2);

    assertThat(loggerInterceptionParameter.getParameters().isEmpty(), is(true));
    assertThat(flowRefInterceptionParameter.getParameters().get("name"), is("anotherFlow"));
    assertThat(splitterInterceptionParameter.getParameters().get("expression"), nullValue());
  }

  @Test
  public void operationParameters() throws Exception {
    flowRunner("killFromPayload").withPayload("T-1000").withVariable("goodbye", "Hasta la vista, baby").run();

    assertThat(HasInjectedAttributesInterceptor.interceptionParameters.size(), is(1));
    List<InterceptionParameters> interceptionParameters = HasInjectedAttributesInterceptor.interceptionParameters;

    InterceptionParameters killInterceptionParameter = interceptionParameters.get(0);

    assertThat(killInterceptionParameter.getParameters().keySet(), contains("victim", "goodbyeMessage"));
    assertThat(killInterceptionParameter.getParameters().get("victim"), is("T-1000"));
    assertThat(killInterceptionParameter.getParameters().get("goodbyeMessage"), is("Hasta la vista, baby"));
  }

  @Test
  public void resolvedConfigOperationParameters() throws Exception {
    flowRunner("die").run();

    assertThat(HasInjectedAttributesInterceptor.interceptionParameters.size(), is(1));
    List<InterceptionParameters> interceptionParameters = HasInjectedAttributesInterceptor.interceptionParameters;

    InterceptionParameters dieInterceptionParameter = interceptionParameters.get(0);

    assertThat(dieInterceptionParameter.getParameters().keySet(), contains("config-ref", "config"));
    final Object config = dieInterceptionParameter.getParameters().get("config");
    assertThat(config, instanceOf(HeisenbergExtension.class));
    assertThat(((HeisenbergExtension) config).getConfigName(), is("heisenberg"));

    final Object configRef = dieInterceptionParameter.getParameters().get("config-ref");
    assertThat(configRef, is("heisenberg"));
  }

  @Test
  public void resolvedComplexParametersOperationParameters() throws Exception {
    flowRunner("killWithCustomMessage").withVariable("goodbye", "Hasta la vista, baby").run();

    assertThat(HasInjectedAttributesInterceptor.interceptionParameters.size(), is(1));
    List<InterceptionParameters> interceptionParameters = HasInjectedAttributesInterceptor.interceptionParameters;

    InterceptionParameters killInterceptionParameter = interceptionParameters.get(0);

    assertThat(killInterceptionParameter.getParameters().keySet(), contains("victim", "goodbyeMessage", "killParameters"));
    assertThat(killInterceptionParameter.getParameters().get("victim"), is("T-1000"));
    assertThat(killInterceptionParameter.getParameters().get("goodbyeMessage"), is("Hasta la vista, baby"));
    assertThat(killInterceptionParameter.getParameters().get("killParameters"), is(instanceOf(KillParameters.class)));
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

    private static final List<InterceptionParameters> interceptionParameters = new LinkedList<>();

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
      interceptionParameters.add(new InterceptionParameters(parameters, event));
      assertThat(expressionEvaluator, not(nullValue()));
      assertThat(lockFactory, not(nullValue()));
      assertThat(httpService, not(nullValue()));
    }
  }

  public static class InterceptionParameters {

    private Map<String, Object> parameters;
    private InterceptionEvent event;

    public InterceptionParameters(Map<String, Object> parameters, InterceptionEvent event) {
      this.parameters = parameters;
      this.event = event;
    }

    public Map<String, Object> getParameters() {
      return parameters;
    }

    public InterceptionEvent getEvent() {
      return event;
    }
  }

}

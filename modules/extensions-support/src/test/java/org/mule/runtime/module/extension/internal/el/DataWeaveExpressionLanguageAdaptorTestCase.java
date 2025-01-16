/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.el;

import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.api.metadata.DataType.OBJECT;
import static org.mule.runtime.api.metadata.DataType.fromType;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.from;
import static org.mule.runtime.module.extension.internal.component.AnnotatedObjectInvocationHandler.addAnnotationsToClass;
import static org.mule.test.allure.AllureConstants.ExpressionLanguageFeature.EXPRESSION_LANGUAGE;
import static org.mule.test.allure.AllureConstants.ExpressionLanguageFeature.ExpressionLanguageStory.SUPPORT_DW;

import static java.nio.charset.Charset.defaultCharset;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.DefaultExpressionLanguageFactoryService;
import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.el.dataweave.DataWeaveExpressionLanguageAdaptor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.weave.v2.el.provider.WeaveDefaultExpressionLanguageFactoryService;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(EXPRESSION_LANGUAGE)
@Story(SUPPORT_DW)
public class DataWeaveExpressionLanguageAdaptorTestCase extends AbstractMuleContextTestCase {

  protected DataWeaveExpressionLanguageAdaptor expressionLanguage;

  private DefaultExpressionLanguageFactoryService weaveExpressionExecutor;
  protected Registry registry = mock(Registry.class);

  @Before
  public void setUp() throws InitialisationException {
    weaveExpressionExecutor = new WeaveDefaultExpressionLanguageFactoryService(null);
    when(registry.lookupByType(DefaultExpressionLanguageFactoryService.class)).thenReturn(of(weaveExpressionExecutor));
    expressionLanguage =
        new DataWeaveExpressionLanguageAdaptor(muleContext, registry, muleContext.getConfiguration(),
                                               () -> defaultCharset(),
                                               weaveExpressionExecutor, getFeatureFlaggingService());
    expressionLanguage.initialise();
  }

  private static final int GC_POLLING_TIMEOUT = 10000;

  private final ExpressionLanguage genericExpressionLanguage = spy(ExpressionLanguage.class);
  private DefaultExpressionLanguageFactoryService genericExpressionLanguageService;
  private final BindingContext bindingContext = NULL_BINDING_CONTEXT;

  @Before
  public void before() {
    genericExpressionLanguageService = mock(DefaultExpressionLanguageFactoryService.class);
    when(genericExpressionLanguageService.create(any())).thenReturn(genericExpressionLanguage);
  }

  @Test
  public void accessRegistryDynamicallyAnnotatedBean() throws Exception {
    CoreEvent event = testEvent();

    MyBean annotatedMyBean = (MyBean) addAnnotationsToClass(MyBean.class).newInstance();
    annotatedMyBean.setName("DataWeave");
    when(registry.lookupByName("myBean")).thenReturn(of(annotatedMyBean));
    TypedValue<?> evaluate = expressionLanguage.evaluate("app.registry.myBean", fromType(MyBean.class), event,
                                                         from("flow"), BindingContext.builder().build(), false);
    assertThat(evaluate.getValue(), is(instanceOf(MyBean.class)));
  }

  private CoreEvent getEventWithError(Optional<Error> error) {
    CoreEvent event = mock(CoreEvent.class, RETURNS_DEEP_STUBS);
    doReturn(error).when(event).getError();
    when(event.getMessage().getPayload()).thenReturn(new TypedValue<>(null, OBJECT));
    when(event.getMessage().getAttributes()).thenReturn(new TypedValue<>(null, OBJECT));
    when(event.getAuthentication()).thenReturn(empty());
    when(event.getItemSequenceInfo()).thenReturn(empty());
    return event;
  }

  public static class MyBean {

    private String name;

    public MyBean() {}

    public MyBean(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

}

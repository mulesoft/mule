/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el;

import static org.mule.runtime.api.el.BindingContext.builder;
import static org.mule.runtime.api.metadata.DataType.NUMBER;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.runtime.api.metadata.DataType.fromFunction;
import static org.mule.runtime.api.metadata.DataType.fromType;
import static org.mule.test.allure.AllureConstants.ExpressionLanguageFeature.EXPRESSION_LANGUAGE;
import static org.mule.test.allure.AllureConstants.ExpressionLanguageFeature.ExpressionLanguageStory.SUPPORT_FUNCTIONS;
import static java.util.Optional.of;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.DefaultExpressionLanguageFactoryService;
import org.mule.runtime.api.el.ExpressionFunction;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.FunctionParameter;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.el.GlobalBindingContextProvider;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.api.config.builders.DefaultsConfigurationBuilder;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.weave.v2.el.WeaveDefaultExpressionLanguageFactoryService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(EXPRESSION_LANGUAGE)
@Story(SUPPORT_FUNCTIONS)
public class GlobalBindingContextProviderTestCase extends AbstractMuleContextTestCase {

  public static final String KEY = "testProvider";


  @Override
  protected ConfigurationBuilder getBuilder() throws Exception {
    return new DefaultsConfigurationBuilder() {

      @Override
      public void configure(MuleContext muleContext) throws ConfigurationException {
        try {
          DefaultExpressionLanguageFactoryService weaveExpressionExecutor = new WeaveDefaultExpressionLanguageFactoryService();
          muleContext.getRegistry().registerObject(weaveExpressionExecutor.getName(), weaveExpressionExecutor);
        } catch (RegistrationException e) {
          throw new ConfigurationException(e);
        }
        super.configure(muleContext);
        try {
          muleContext.getRegistry().registerObject(KEY, new TestGlobalBindingContextProvider());
        } catch (RegistrationException e) {
          throw new ConfigurationException(e);
        }
      }
    };
  }

  @Test
  public void variable() {
    TypedValue result = muleContext.getExpressionManager().evaluate("number");
    assertThat(result.getValue(), is(1));
    assertThat(NUMBER.isCompatibleWith(result.getDataType()), is(true));
  }

  @Test
  public void function() {
    TypedValue result = muleContext.getExpressionManager().evaluate("repeat('oa', 3)");
    assertThat(result.getValue(), is("oaoaoa"));
    assertThat(result.getDataType(), is(STRING));
  }

  private class TestGlobalBindingContextProvider implements GlobalBindingContextProvider {

    @Override
    public BindingContext getBindingContext() {
      TestExpressionFunction function = new TestExpressionFunction();
      return builder()
          .addBinding("number", new TypedValue(1, NUMBER))
          .addBinding("repeat", new TypedValue(function, fromFunction(function)))
          .build();
    }
  }

  private class TestExpressionFunction implements ExpressionFunction {

    @Override
    public Object call(Object[] objects, BindingContext bindingContext) {
      StringBuilder builder = new StringBuilder();
      for (int i = 0; i < (Integer) objects[1]; i++) {
        builder.append((String) objects[0]);
      }
      return builder.toString();
    }

    @Override
    public Optional<DataType> returnType() {
      return of(STRING);
    }

    @Override
    public List<FunctionParameter> parameters() {
      List<FunctionParameter> parameters = new ArrayList<>();
      parameters.add(new FunctionParameter("word", STRING));
      parameters.add(new FunctionParameter("times", fromType(Integer.class), ctx -> 1));
      return parameters;
    }

  }
}

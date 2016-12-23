/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el;

import static java.util.Optional.of;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.el.BindingContext.builder;
import static org.mule.runtime.api.metadata.DataType.NUMBER;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.runtime.api.metadata.DataType.fromFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.ExpressionFunction;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.FunctionDataType;
import org.mule.runtime.api.metadata.FunctionParameter;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.el.GlobalBindingContextProvider;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.config.builders.DefaultsConfigurationBuilder;
import org.mule.runtime.core.metadata.DefaultTypedValue;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Expression Language")
@Stories("Support Functions")
public class GlobalBindingContextProviderTestCase extends AbstractMuleContextTestCase {

  public static final String KEY = "testProvider";

  @Override
  protected ConfigurationBuilder getBuilder() throws Exception {
    return new DefaultsConfigurationBuilder() {

      @Override
      public void configure(MuleContext muleContext) throws ConfigurationException {
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
    TypedValue result = muleContext.getExpressionManager().evaluate("dw:number");
    assertThat(result.getValue(), is(1));
    assertThat(NUMBER.isCompatibleWith(result.getDataType()), is(true));
  }

  @Test
  public void function() {
    // For now, only verify we are passing the function along correctly and it's working as expected
    GlobalBindingContextProvider provider = muleContext.getRegistry().lookupObject(KEY);
    Optional<TypedValue> binding = provider.getBindingContext().lookup("repeat");
    assertThat(binding.isPresent(), is(true));

    assertThat(binding.get().getDataType(), instanceOf(FunctionDataType.class));

    assertThat(binding.get().getValue(), instanceOf(ExpressionFunction.class));
    assertThat(((ExpressionFunction) binding.get().getValue()).call(new Object[] {"oa", 2}, builder().build()),
               is("oaoa"));
  }

  private class TestGlobalBindingContextProvider implements GlobalBindingContextProvider {

    @Override
    public BindingContext getBindingContext() {
      TestExpressionFunction function = new TestExpressionFunction();
      return builder()
          .addBinding("number", new DefaultTypedValue(1, NUMBER))
          .addBinding("repeat", new DefaultTypedValue(function, fromFunction(function)))
          .build();
    }
  }

  private class TestExpressionFunction implements ExpressionFunction {

    @Override
    public Object call(Object[] objects, BindingContext bindingContext) {
      StringBuilder builder = new StringBuilder();
      for (int i = 0; i < (int) objects[1]; i++) {
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
      parameters.add(new FunctionParameter("times", NUMBER, ctx -> 1));
      return parameters;
    }

  }
}

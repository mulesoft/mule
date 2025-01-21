/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetUtils.getResolverSetFromParameters;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.ParameterDslConfiguration;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.parameterization.ComponentParameterization;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.module.extension.api.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.resolver.ValueResolverFactory;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.test.module.extension.internal.util.ExtensionsTestUtils;

import org.junit.Test;

import io.qameta.allure.Issue;

public class ResolverSetUtilsTestCase extends AbstractMuleTestCase {

  @Test
  @Issue("W-17579307")
  public void pojoFromComponentParameterization() throws MuleException {
    MetadataType pojoType = ExtensionsTestUtils.TYPE_LOADER.load(MyPojo.class);
    ParameterizedModel pmzdModel = createModelWithPojoPram(pojoType);
    ParameterizedModel pojoModel = createPojoModel();

    ComponentParameterization<ParameterizedModel> componentParameterization =
        ComponentParameterization.builder(pojoModel)
            .withParameter("textValue", "hello me")
            .withParameter("numberValue", 42)
            .build();

    ResolverSet resolvers = getResolverSetFromParameters(pmzdModel,
                                                         (pgm, pm) -> componentParameterization,
                                                         createMuleContext(),
                                                         true,
                                                         new ReflectionCache(),
                                                         null, "",
                                                         new ValueResolverFactory());
    ValueResolver<?> paramResolver = resolvers.getResolvers().get("paramName");
    Object resolvedValue = paramResolver.resolve(null);

    assertThat(resolvedValue, instanceOf(MyPojo.class));
    assertThat(((MyPojo) resolvedValue).getTextValue(), is("hello me"));
    assertThat(((MyPojo) resolvedValue).getNumberValue(), is(42));
  }

  private ParameterizedModel createModelWithPojoPram(MetadataType pojoType) {
    ParameterModel paramModel = mock(ParameterModel.class);
    when(paramModel.getName()).thenReturn("paramName");
    when(paramModel.getType()).thenReturn(pojoType);
    when(paramModel.getModelProperties()).thenReturn(emptySet());
    when(paramModel.getDslConfiguration()).thenReturn(new ParameterDslConfiguration());

    ParameterGroupModel paramGroupModel = mock(ParameterGroupModel.class);
    when(paramGroupModel.getParameterModels()).thenReturn(singletonList(paramModel));

    ParameterizedModel pmzdModel = mock(ParameterizedModel.class);
    when(pmzdModel.getParameterGroupModels()).thenReturn(singletonList(paramGroupModel));
    when(pmzdModel.getAllParameterModels()).thenReturn(singletonList(paramModel));
    return pmzdModel;
  }

  private ParameterizedModel createPojoModel() {
    ParameterModel pojoTextParamModel = mock(ParameterModel.class);
    when(pojoTextParamModel.getName()).thenReturn("textValue");
    when(pojoTextParamModel.getType()).thenReturn(ExtensionsTestUtils.TYPE_LOADER.load(String.class));
    when(pojoTextParamModel.getModelProperties()).thenReturn(emptySet());
    when(pojoTextParamModel.getDslConfiguration()).thenReturn(new ParameterDslConfiguration());

    ParameterModel pojoNumberParamModel = mock(ParameterModel.class);
    when(pojoNumberParamModel.getName()).thenReturn("numberValue");
    when(pojoNumberParamModel.getType()).thenReturn(ExtensionsTestUtils.TYPE_LOADER.load(String.class));
    when(pojoNumberParamModel.getModelProperties()).thenReturn(emptySet());
    when(pojoNumberParamModel.getDslConfiguration()).thenReturn(new ParameterDslConfiguration());

    ParameterGroupModel pojoParamGroupModel = mock(ParameterGroupModel.class);
    when(pojoParamGroupModel.getName()).thenReturn("MyPojo");
    when(pojoParamGroupModel.getParameterModels()).thenReturn(singletonList(pojoTextParamModel));
    when(pojoParamGroupModel.getParameter("textValue")).thenReturn(of(pojoTextParamModel));
    when(pojoParamGroupModel.getParameter("numberValue")).thenReturn(of(pojoNumberParamModel));

    ParameterizedModel pojoModel = mock(ParameterizedModel.class);
    when(pojoModel.getParameterGroupModels()).thenReturn(singletonList(pojoParamGroupModel));
    return pojoModel;
  }

  private MuleContext createMuleContext() {
    MuleContext muleContext = mock(MuleContext.class);
    when(muleContext.getInjector()).thenReturn(mock(Injector.class));
    return muleContext;
  }

  public static class MyPojo {

    private String textValue;

    private int numberValue;

    public String getTextValue() {
      return textValue;
    }

    public void setTextValue(String textValue) {
      this.textValue = textValue;
    }

    public int getNumberValue() {
      return numberValue;
    }

    public void setNumberValue(int numberValue) {
      this.numberValue = numberValue;
    }

  }
}

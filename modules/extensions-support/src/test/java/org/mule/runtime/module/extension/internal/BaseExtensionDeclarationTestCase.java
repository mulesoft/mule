/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.when;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.runtime.extension.api.introspection.ExtensionFactory;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.declaration.DescribingContext;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.spi.ModelEnricher;
import org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport;
import org.mule.runtime.extension.api.introspection.parameter.ParameterModel;
import org.mule.runtime.module.extension.internal.introspection.DefaultExtensionFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Collection;
import java.util.Collections;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
abstract class BaseExtensionDeclarationTestCase extends AbstractMuleTestCase {

  @Mock
  protected ServiceRegistry serviceRegistry;

  protected ExtensionDeclarer extensionDeclarer;
  protected ExtensionModel extensionModel;
  protected ExtensionFactory factory;

  @Before
  public void buildExtension() throws Exception {
    Collection<ModelEnricher> emptyList = Collections.emptyList();
    when(serviceRegistry.lookupProviders(same(ModelEnricher.class))).thenReturn(emptyList);
    when(serviceRegistry.lookupProviders(same(ModelEnricher.class), any(ClassLoader.class))).thenReturn(emptyList);

    factory = new DefaultExtensionFactory(serviceRegistry, getClass().getClassLoader());
    extensionDeclarer = createDeclarationDescriptor();
    extensionModel = factory.createFrom(extensionDeclarer, createDescribingContext());
  }

  protected DescribingContext createDescribingContext() {
    return new DefaultDescribingContext(extensionDeclarer, getClass().getClassLoader());
  }

  protected void assertParameter(ParameterModel parameterModel, String name, String description,
                                 ExpressionSupport expressionSupport, boolean required, MetadataType metadataType,
                                 Class<? extends MetadataType> qualifier, Object defaultValue) {
    assertThat(parameterModel, is(notNullValue()));
    assertThat(parameterModel.getName(), equalTo(name));
    assertThat(parameterModel.getDescription(), equalTo(description));
    assertThat(parameterModel.getExpressionSupport(), is(expressionSupport));
    assertThat(parameterModel.isRequired(), is(required));
    assertThat(parameterModel.getType(), equalTo(metadataType));
    assertThat(parameterModel.getType(), is(instanceOf(qualifier)));

    if (defaultValue != null) {
      assertThat(parameterModel.getDefaultValue(), equalTo(defaultValue));
    } else {
      assertThat(parameterModel.getDefaultValue(), is(nullValue()));
    }
  }

  protected void assertDataType(MetadataType metadataType, Class<?> expectedRawType,
                                Class<? extends MetadataType> typeQualifier) {
    assertThat(metadataType, is(instanceOf(typeQualifier)));
    assertThat(expectedRawType.isAssignableFrom(getType(metadataType)), is(true));
  }

  protected abstract ExtensionDeclarer createDeclarationDescriptor();

}

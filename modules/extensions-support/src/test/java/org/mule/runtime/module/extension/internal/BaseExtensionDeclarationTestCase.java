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
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectionProviderDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.runtime.extension.api.declaration.DescribingContext;
import org.mule.runtime.extension.api.declaration.spi.ModelEnricher;
import org.mule.runtime.extension.api.runtime.ExtensionFactory;
import org.mule.runtime.extension.api.runtime.config.ConfigurationFactory;
import org.mule.runtime.extension.api.runtime.operation.OperationExecutorFactory;
import org.mule.runtime.module.extension.internal.introspection.DefaultExtensionFactory;
import org.mule.runtime.module.extension.internal.model.property.ConfigurationFactoryModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ConnectionTypeModelProperty;
import org.mule.runtime.module.extension.internal.model.property.OperationExecutorModelProperty;
import org.mule.runtime.module.extension.internal.util.IdempotentDeclarationWalker;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Collection;
import java.util.Collections;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public abstract class BaseExtensionDeclarationTestCase extends AbstractMuleTestCase {

  @Mock
  protected ServiceRegistry serviceRegistry;

  protected ExtensionDeclarer extensionDeclarer;
  protected ExtensionModel extensionModel;
  protected ExtensionFactory factory;

  @Before
  public void buildExtension() throws Exception {
    Collection<ModelEnricher> emptyList = Collections.emptyList();
    when(serviceRegistry.lookupProviders(same(ModelEnricher.class), any(ClassLoader.class))).thenReturn(emptyList);

    factory = new DefaultExtensionFactory(serviceRegistry, getClass().getClassLoader());
    extensionDeclarer = enrich(createDeclarationDescriptor());
    extensionModel = factory.createFrom(extensionDeclarer, createDescribingContext());
  }

  private ExtensionDeclarer enrich(ExtensionDeclarer declarer) {
    new IdempotentDeclarationWalker() {

      @Override
      public void onConfiguration(ConfigurationDeclaration declaration) {
        declaration.addModelProperty(new ConfigurationFactoryModelProperty(mock(ConfigurationFactory.class, RETURNS_DEEP_STUBS)));
      }

      @Override
      protected void onOperation(OperationDeclaration declaration) {
        declaration
            .addModelProperty(new OperationExecutorModelProperty(mock(OperationExecutorFactory.class, RETURNS_DEEP_STUBS)));
      }

      @Override
      protected void onConnectionProvider(ConnectionProviderDeclaration declaration) {
        declaration.addModelProperty(new ConnectionTypeModelProperty(Object.class));
      }
    }.walk(declarer.getDeclaration());

    return declarer;
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
    assertThat(getType(parameterModel.getType()), equalTo(getType(metadataType)));
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

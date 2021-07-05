/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.resolver;

import static java.util.Collections.emptySet;
import static java.util.Optional.of;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.api.meta.ExpressionSupport.SUPPORTED;

import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.impl.DefaultStringType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.extension.api.property.ClassLoaderModelProperty;
import org.mule.tck.classlaoder.TestClassLoader;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

public class ValueResolverFactoryTestCase {

  private final String CLASS_NAME = "java.lang.String";
  private final Class<?> PLUGIN_LOADED_CLASS = String.class;

  private TestClassLoader pluginClassLoader;

  private final ValueResolverFactory valueResolverFactory = new ValueResolverFactory(mock(DslSyntaxResolver.class));

  @Before
  public void setup() {
    pluginClassLoader = spy(new TestClassLoader(null));
  }

  @Test
  public void loadsClassFromExtensionFirst() throws Exception {
    pluginClassLoader.addClass(CLASS_NAME, PLUGIN_LOADED_CLASS);

    ExtensionModel extensionModel = mock(ExtensionModel.class);
    Optional<ClassLoaderModelProperty> classLoaderModelProperty = of(mock(ClassLoaderModelProperty.class));
    DefaultStringType defaultStringType = BaseTypeBuilder.create(JAVA).stringType().build();

    when(classLoaderModelProperty.get().getClassLoader()).thenReturn(pluginClassLoader);
    when(extensionModel.getModelProperty(ClassLoaderModelProperty.class)).thenReturn(classLoaderModelProperty);

    valueResolverFactory.of(extensionModel, "parameterName", defaultStringType, "value",
                            "defaultValue", SUPPORTED, true, emptySet());

    verify(pluginClassLoader, times(1)).loadClass(CLASS_NAME);
  }

}

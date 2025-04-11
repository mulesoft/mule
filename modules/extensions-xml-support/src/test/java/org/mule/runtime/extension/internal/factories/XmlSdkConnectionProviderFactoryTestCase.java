/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.factories;

import static org.mule.test.allure.AllureConstants.XmlSdk.XML_SDK;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.function.UnaryOperator.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.concat;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.hamcrest.collection.IsMapWithSize.aMapWithSize;
import static org.hamcrest.collection.IsMapWithSize.anEmptyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

import io.qameta.allure.Feature;

@Feature(XML_SDK)
public class XmlSdkConnectionProviderFactoryTestCase extends AbstractMuleTestCase {

  private XmlSdkConfigurationFactory configurationFactory;
  private ParameterDeclaration somePropertyParameterDeclaration;

  @Before
  public void before() {
    configurationFactory = new XmlSdkConfigurationFactory(emptyList());
    somePropertyParameterDeclaration = new ParameterDeclaration("someProperty");
  }

  @Test
  public void noConnectionProperties() {
    final Class connectionProviderObjectType = new XmlSdkConnectionProviderFactory(connectionProviderWithFixedValue(),
                                                                                   emptyList(),
                                                                                   emptyList(),
                                                                                   configurationFactory)
        .getObjectType();

    final Map<String, Method> gettersFrom = gettersFrom(connectionProviderObjectType);
    assertThat(gettersFrom, anEmptyMap());
  }

  @Test
  public void propertyParam() {
    final Class connectionProviderObjectType =
        new XmlSdkConnectionProviderFactory(connectionProviderWithPropertyValue("someProperty"),
                                            emptyList(),
                                            asList(somePropertyParameterDeclaration),
                                            configurationFactory)
            .getObjectType();

    final Map<String, Method> gettersFrom = gettersFrom(connectionProviderObjectType);
    assertThat(gettersFrom, aMapWithSize(1));
    assertThat(gettersFrom, hasKey("getSomeProperty"));
  }

  @Test
  public void propertyParamFromConfig() {
    final Class connectionProviderObjectType =
        new XmlSdkConnectionProviderFactory(connectionProviderWithPropertyValue("someProperty"),
                                            asList(somePropertyParameterDeclaration),
                                            emptyList(),
                                            configurationFactory)
            .getObjectType();

    final Map<String, Method> gettersFrom = gettersFrom(connectionProviderObjectType);
    assertThat(gettersFrom, aMapWithSize(1));
    assertThat(gettersFrom, hasKey("getSomeProperty"));
  }

  @Test
  public void propertyParamFromConfigColliding() {
    final Class connectionProviderObjectType =
        new XmlSdkConnectionProviderFactory(connectionProviderWithPropertyValue("someProperty"),
                                            asList(somePropertyParameterDeclaration),
                                            asList(somePropertyParameterDeclaration),
                                            configurationFactory)
            .getObjectType();

    final Map<String, Method> gettersFrom = gettersFrom(connectionProviderObjectType);
    assertThat(gettersFrom, aMapWithSize(1));
    assertThat(gettersFrom, hasKey("getSomeProperty"));
  }

  @Test
  public void complexParam() {
    final Class connectionProviderObjectType =
        new XmlSdkConnectionProviderFactory(connectionProviderWithComplexParamWithPropertyValue("someProperty"),
                                            emptyList(),
                                            asList(somePropertyParameterDeclaration),
                                            configurationFactory)
            .getObjectType();

    final Map<String, Method> gettersFrom = gettersFrom(connectionProviderObjectType);
    assertThat(gettersFrom, aMapWithSize(1));
    assertThat(gettersFrom, hasKey("getSomeProperty"));
  }

  @Test
  public void usedValueFromConfigPropagatedToConnectionProvider() throws Exception {
    final XmlSdkConfigurationFactory configFactory = new XmlSdkConfigurationFactory(asList(somePropertyParameterDeclaration));
    final Object config = configFactory.newInstance();

    config.getClass().getDeclaredMethod("setSomeProperty", String.class)
        .invoke(config, "myValue");

    final ConnectionProvider connectionProvider =
        new XmlSdkConnectionProviderFactory(connectionProviderWithPropertyValue("someProperty"),
                                            asList(somePropertyParameterDeclaration),
                                            emptyList(),
                                            configFactory)
            .newInstance();

    final Object somePropertyValueConnection = connectionProvider.getClass().getDeclaredMethod("getSomeProperty")
        .invoke(connectionProvider);

    assertThat(somePropertyValueConnection, is("myValue"));
    assertThat(configFactory.getLastBuilt(), is(nullValue()));
  }

  private ComponentAst connectionProviderWithFixedValue() {
    final ComponentParameterAst fixedValueParam = mock(ComponentParameterAst.class);
    when(fixedValueParam.getRawValue()).thenReturn("fixedValue");

    final ComponentAst innerConnectionProviderComponent = mock(ComponentAst.class);
    when(innerConnectionProviderComponent.getParameters())
        .thenReturn(singleton(fixedValueParam));

    when(innerConnectionProviderComponent.recursiveStream())
        .thenReturn(Stream.of(innerConnectionProviderComponent));
    return innerConnectionProviderComponent;
  }

  private ComponentAst connectionProviderWithPropertyValue(final String propertyName) {
    final ComponentParameterAst propertyParam = mock(ComponentParameterAst.class);
    when(propertyParam.getRawValue()).thenReturn("#[vars." + propertyName + "]");

    final ComponentAst innerConnectionProviderComponent = mock(ComponentAst.class);
    when(innerConnectionProviderComponent.getParameters())
        .thenReturn(singleton(propertyParam));

    when(innerConnectionProviderComponent.recursiveStream())
        .thenReturn(Stream.of(innerConnectionProviderComponent));
    return innerConnectionProviderComponent;
  }

  private ComponentAst connectionProviderWithComplexParamWithPropertyValue(final String propertyName) {
    final ComponentParameterAst propertyParam = mock(ComponentParameterAst.class);
    when(propertyParam.getRawValue()).thenReturn("#[vars." + propertyName + "]");

    final ComponentAst innerConnectionProviderComponentObject = mock(ComponentAst.class);
    when(innerConnectionProviderComponentObject.getParameters())
        .thenReturn(singleton(propertyParam));

    final ComponentAst innerConnectionProviderComponent = mock(ComponentAst.class);

    when(innerConnectionProviderComponent.recursiveStream())
        .thenReturn(concat(Stream.of(innerConnectionProviderComponent),
                           Stream.of(innerConnectionProviderComponentObject)));

    return innerConnectionProviderComponent;
  }

  private Map<String, Method> gettersFrom(final Class connectionProviderObjectType) {
    return Stream.of(connectionProviderObjectType.getDeclaredMethods())
        .filter(m -> m.getName().startsWith("get") && m.getParameters().length == 0)
        .collect(toMap(m -> m.getName(), identity()));
  }

}

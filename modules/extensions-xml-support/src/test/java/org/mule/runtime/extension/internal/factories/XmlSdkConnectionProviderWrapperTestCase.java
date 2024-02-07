/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.factories;

import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.api.functional.Either.right;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.test.allure.AllureConstants.XmlSdk.XML_SDK;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Optional.of;
import static java.util.stream.Stream.concat;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.builder.ObjectTypeBuilder;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.MetadataTypeAdapter;
import org.mule.runtime.extension.api.runtime.connectivity.ConnectionProviderFactory;
import org.mule.runtime.module.extension.internal.loader.java.property.ConnectionProviderFactoryModelProperty;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

import io.qameta.allure.Feature;

@Feature(XML_SDK)
public class XmlSdkConnectionProviderWrapperTestCase extends AbstractMuleContextTestCase {

  private ExtensionModel extensionModel;

  private TestConnectionProvider targetInstance;
  private ObjectType complexType;
  private ConnectionProviderFactory connectionProviderFactory;
  private ConnectionProviderModel connectionProviderModel;

  @Before
  public void before() {
    extensionModel = mock(ExtensionModel.class);
    final XmlDslModel xmlDslModel = XmlDslModel.builder()
        .setPrefix("test")
        .build();
    when(extensionModel.getXmlDslModel()).thenReturn(xmlDslModel);

    final ObjectTypeBuilder complexTypeBuilder = BaseTypeBuilder.create(JAVA).objectType()
        .with(new ClassInformationAnnotation("org.mule.runtime.extension.internal.factories.XmlSdkConnectionProviderWrapperTestCase.TestConnectionProvider",
                                             true, false, true, false, false, emptyList(), null, emptyList(), false));
    complexTypeBuilder.addField().key("someProperty")
        .value(BaseTypeBuilder.create(JAVA).stringType().build());
    complexType = complexTypeBuilder.build();

    targetInstance = new TestConnectionProvider();

    connectionProviderFactory = mock(ConnectionProviderFactory.class);
    when(connectionProviderFactory.newInstance()).thenReturn(targetInstance);

    final ParameterModel somePropertyParamModel = mock(ParameterModel.class);
    when(somePropertyParamModel.getName()).thenReturn("someProperty");

    final ParameterModel objectParamModel = mock(ParameterModel.class);
    when(objectParamModel.getName()).thenReturn("object");
    when(objectParamModel.getType()).thenReturn(complexType);

    connectionProviderModel = mock(ConnectionProviderModel.class);
    when(connectionProviderModel.getModelProperty(ConnectionProviderFactoryModelProperty.class))
        .thenReturn(of(new ConnectionProviderFactoryModelProperty(connectionProviderFactory)));
    final ParameterGroupModel paramGroupModel = mock(ParameterGroupModel.class);
    when(paramGroupModel.getParameterModels()).thenReturn(asList(somePropertyParamModel, objectParamModel));
    when(paramGroupModel.getName()).thenReturn(DEFAULT_GROUP_NAME);
    when(connectionProviderModel.getParameterGroupModels()).thenReturn(asList(paramGroupModel));
    when(connectionProviderModel.getAllParameterModels()).thenReturn(asList(somePropertyParamModel, objectParamModel));
  }

  @Test
  public void providerWithSimpleProperty() throws InitialisationException {
    ComponentAst innerConnectionProviderComponent = connectionProviderWithPropertyValue("someProperty");
    when(innerConnectionProviderComponent.getModel(ConnectionProviderModel.class))
        .thenReturn(of(connectionProviderModel));
    when(innerConnectionProviderComponent.getExtensionModel())
        .thenReturn(extensionModel);

    final TestConnectionProviderWrapper connectionProvider =
        new TestConnectionProviderWrapper(innerConnectionProviderComponent, p -> of("someProperty"));
    connectionProvider.setSomeProperty("myValue");
    initialiseIfNeeded(connectionProvider, true, muleContext);

    verify(connectionProviderFactory).newInstance();

    assertThat(targetInstance.getSomeProperty(), is("myValue"));
  }

  @Test
  public void providerWithComplexProperty() throws InitialisationException {
    ComponentAst innerConnectionProviderComponent = connectionProviderWithComplexParamWithPropertyValue("someProperty");
    when(innerConnectionProviderComponent.getModel(ConnectionProviderModel.class))
        .thenReturn(of(connectionProviderModel));
    when(innerConnectionProviderComponent.getExtensionModel())
        .thenReturn(extensionModel);

    final TestConnectionProviderWrapper connectionProvider =
        new TestConnectionProviderWrapper(innerConnectionProviderComponent, p -> of("someProperty"));
    connectionProvider.setSomeProperty("myValue");
    initialiseIfNeeded(connectionProvider, true, muleContext);

    verify(connectionProviderFactory).newInstance();

    TestConnectionProvider targetNestedObject = (TestConnectionProvider) targetInstance.getObject();
    assertThat(targetNestedObject.getSomeProperty(), is("myValue"));
  }

  private ComponentAst connectionProviderWithPropertyValue(final String propertyName) {
    final ParameterModel somePropertyParameterModel = mock(ParameterModel.class);
    when(somePropertyParameterModel.getName()).thenReturn(propertyName);
    when(somePropertyParameterModel.getType()).thenReturn(BaseTypeBuilder.create(JAVA).stringType().build());

    final ComponentParameterAst propertyParam = mock(ComponentParameterAst.class);
    when(propertyParam.getRawValue()).thenReturn("#[vars." + propertyName + "]");
    when(propertyParam.getModel()).thenReturn(somePropertyParameterModel);

    final ComponentAst innerConnectionProviderComponent = mock(ComponentAst.class);
    when(innerConnectionProviderComponent.getParameters())
        .thenReturn(singleton(propertyParam));

    when(innerConnectionProviderComponent.recursiveStream())
        .thenReturn(Stream.of(innerConnectionProviderComponent));
    return innerConnectionProviderComponent;
  }

  private ComponentAst connectionProviderWithComplexParamWithPropertyValue(final String propertyName) {
    final ParameterModel somePropertyParameterModel = mock(ParameterModel.class);
    when(somePropertyParameterModel.getName()).thenReturn(propertyName);
    when(somePropertyParameterModel.getType()).thenReturn(BaseTypeBuilder.create(JAVA).stringType().build());

    final ComponentParameterAst propertyParam = mock(ComponentParameterAst.class);
    when(propertyParam.getRawValue()).thenReturn("#[vars." + propertyName + "]");
    when(propertyParam.getModel()).thenReturn(somePropertyParameterModel);

    final ComponentAst innerConnectionProviderComponentObject = mock(ComponentAst.class);
    when(innerConnectionProviderComponentObject.getParameters()).thenReturn(singleton(propertyParam));

    final MetadataTypeAdapter complexTypeAdapter = mock(MetadataTypeAdapter.class);
    when(complexTypeAdapter.getType()).thenReturn(complexType);
    when(innerConnectionProviderComponentObject.getModel(MetadataTypeAdapter.class))
        .thenReturn(of(complexTypeAdapter));

    final ComponentAst innerConnectionProviderComponent = mock(ComponentAst.class);

    final ParameterModel someObjectParameterModel = mock(ParameterModel.class);
    when(someObjectParameterModel.getName()).thenReturn("object");
    when(someObjectParameterModel.getType()).thenReturn(complexType);

    final ComponentParameterAst complexParam = mock(ComponentParameterAst.class);
    when(complexParam.getRawValue()).thenReturn(null);
    when(complexParam.getValue()).thenReturn(right(innerConnectionProviderComponentObject));
    when(complexParam.getModel()).thenReturn(someObjectParameterModel);

    when(innerConnectionProviderComponent.getParameters()).thenReturn(singleton(complexParam));

    when(innerConnectionProviderComponent.recursiveStream())
        .thenReturn(concat(Stream.of(innerConnectionProviderComponent),
                           Stream.of(innerConnectionProviderComponentObject)));

    return innerConnectionProviderComponent;
  }

  public static class TestConnectionProviderWrapper extends XmlSdkConnectionProviderWrapper {

    private String someProperty;

    public TestConnectionProviderWrapper(ComponentAst innerConnectionProviderComponent, Function propertyUsage) {
      super(innerConnectionProviderComponent, propertyUsage);
    }

    public String getSomeProperty() {
      return someProperty;
    }

    public void setSomeProperty(String someProperty) {
      this.someProperty = someProperty;
    }
  }

  public static class TestConnectionProvider implements ConnectionProvider {

    private Object object;
    private String someProperty;

    public Object getObject() {
      return object;
    }

    public void setObject(Object object) {
      this.object = object;
    }

    public String getSomeProperty() {
      return someProperty;
    }

    public void setSomeProperty(String someProperty) {
      this.someProperty = someProperty;
    }

    @Override
    public Object connect() throws ConnectionException {
      return null;
    }

    @Override
    public void disconnect(Object connection) {}

    @Override
    public ConnectionValidationResult validate(Object connection) {
      return null;
    }
  }
}

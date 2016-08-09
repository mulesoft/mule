/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.enricher;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.reflections.ReflectionUtils.getAllFields;
import static org.reflections.ReflectionUtils.withAnnotation;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.param.ConfigName;
import org.mule.runtime.extension.api.introspection.declaration.DescribingContext;
import org.mule.runtime.extension.api.introspection.declaration.fluent.BaseDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ConnectionProviderDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.module.extension.internal.exception.IllegalConfigurationModelDefinitionException;
import org.mule.runtime.module.extension.internal.model.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.model.property.RequireNameField;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.Apple;

import java.lang.reflect.Field;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ConfigNameModelEnricherTestCase extends AbstractMuleTestCase {

  @Mock(answer = RETURNS_DEEP_STUBS)
  private DescribingContext describingContext;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionDeclarer extensionDeclarer;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionDeclaration extensionDeclaration;

  @Mock
  private ConfigurationDeclaration configurationDeclaration;

  @Mock
  private ConnectionProviderDeclaration connectionProviderDeclaration;

  private Field configNameField;
  private Field providerNameField;
  private final ConfigNameModelEnricher enricher = new ConfigNameModelEnricher();

  @Before
  public void before() throws Exception {
    when(describingContext.getExtensionDeclarer()).thenReturn(extensionDeclarer);
    when(extensionDeclarer.getDeclaration()).thenReturn(extensionDeclaration);
    when(extensionDeclaration.getConfigurations()).thenReturn(asList(configurationDeclaration));
    when(extensionDeclaration.getConnectionProviders()).thenReturn(asList(connectionProviderDeclaration));

    mockImplementingProperty(configurationDeclaration, TestNameAwareConfig.class);
    mockImplementingProperty(connectionProviderDeclaration, TestNameAwareConnectionProvider.class);
    configNameField = getAllFields(TestNameAwareConfig.class, withAnnotation(ConfigName.class)).iterator().next();
    providerNameField = getAllFields(TestNameAwareConnectionProvider.class, withAnnotation(ConfigName.class)).iterator().next();
  }

  @Test
  public void addModelProperty() throws Exception {
    enricher.enrich(describingContext);
    assertModelPropertyAdded(configurationDeclaration, configNameField);
  }

  @Test
  public void addModelPropertyOnConnectionProvider() {
    enricher.enrich(describingContext);
    assertModelPropertyAdded(connectionProviderDeclaration, providerNameField);
  }

  private void assertModelPropertyAdded(BaseDeclaration declaration, Field injectionField) {
    ArgumentCaptor<RequireNameField> captor = ArgumentCaptor.forClass(RequireNameField.class);
    verify(declaration).addModelProperty(captor.capture());

    RequireNameField property = captor.getValue();
    assertThat(property, is(notNullValue()));
    assertThat(property.getConfigNameField(), equalTo(injectionField));
  }

  @Test
  public void configWithoutImplementingProperty() throws Exception {
    mockImplementingProperty(configurationDeclaration, null);
    enricher.enrich(describingContext);
  }

  @Test(expected = IllegalConfigurationModelDefinitionException.class)
  public void manyAnnotatedFields() {
    mockImplementingProperty(configurationDeclaration, TestMultipleNameAwareConfig.class);
    enricher.enrich(describingContext);
  }

  @Test(expected = IllegalConfigurationModelDefinitionException.class)
  public void annotatedFieldOfWrongType() {
    mockImplementingProperty(configurationDeclaration, TestIllegalNameAwareConfig.class);
    enricher.enrich(describingContext);
  }

  private void mockImplementingProperty(BaseDeclaration declaration, Class<?> type) {
    ImplementingTypeModelProperty property = type != null ? new ImplementingTypeModelProperty(type) : null;
    when(declaration.getModelProperty(ImplementingTypeModelProperty.class)).thenReturn(Optional.ofNullable(property));
  }

  public static class TestNameAwareConfig {

    @ConfigName
    private String name;

    public String getName() {
      return name;
    }
  }

  public static class TestMultipleNameAwareConfig {

    @ConfigName
    private String name;

    @ConfigName
    private String redundantName;
  }

  public static class TestIllegalNameAwareConfig {

    @ConfigName
    private Apple name;
  }

  public static class TestNameAwareConnectionProvider implements ConnectionProvider<Object> {

    @ConfigName
    private String name;

    @Override
    public Object connect() throws ConnectionException {
      return new Object();
    }

    @Override
    public void disconnect(Object o) {

    }

    @Override
    public ConnectionValidationResult validate(Object o) {
      return ConnectionValidationResult.success();
    }

    public String getName() {
      return name;
    }
  }
}

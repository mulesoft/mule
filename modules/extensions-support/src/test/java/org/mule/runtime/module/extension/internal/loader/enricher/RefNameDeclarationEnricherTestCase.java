/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

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
import org.mule.runtime.api.meta.model.declaration.fluent.BaseDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectionProviderDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.annotation.param.RefName;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.RequireNameField;
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
public class RefNameDeclarationEnricherTestCase extends AbstractMuleTestCase {

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionLoadingContext extensionLoadingContext;

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
  private final RefNameDeclarationEnricher enricher = new RefNameDeclarationEnricher();

  @Before
  public void before() throws Exception {
    when(extensionLoadingContext.getExtensionDeclarer()).thenReturn(extensionDeclarer);
    when(extensionDeclarer.getDeclaration()).thenReturn(extensionDeclaration);
    when(extensionDeclaration.getConfigurations()).thenReturn(asList(configurationDeclaration));
    when(extensionDeclaration.getConnectionProviders()).thenReturn(asList(connectionProviderDeclaration));

    mockImplementingProperty(configurationDeclaration, TestNameAwareConfig.class);
    mockImplementingProperty(connectionProviderDeclaration, TestNameAwareConnectionProvider.class);
    configNameField = getAllFields(TestNameAwareConfig.class, withAnnotation(RefName.class)).iterator().next();
    providerNameField = getAllFields(TestNameAwareConnectionProvider.class, withAnnotation(RefName.class)).iterator().next();
  }

  @Test
  public void addModelProperty() throws Exception {
    enricher.enrich(extensionLoadingContext);
    assertModelPropertyAdded(configurationDeclaration, configNameField);
  }

  @Test
  public void addModelPropertyOnConnectionProvider() {
    enricher.enrich(extensionLoadingContext);
    assertModelPropertyAdded(connectionProviderDeclaration, providerNameField);
  }

  private void assertModelPropertyAdded(BaseDeclaration declaration, Field injectionField) {
    ArgumentCaptor<RequireNameField> captor = ArgumentCaptor.forClass(RequireNameField.class);
    verify(declaration).addModelProperty(captor.capture());

    RequireNameField property = captor.getValue();
    assertThat(property, is(notNullValue()));
    assertThat(property.getField(), equalTo(injectionField));
  }

  @Test
  public void configWithoutImplementingProperty() throws Exception {
    mockImplementingProperty(configurationDeclaration, null);
    enricher.enrich(extensionLoadingContext);
  }

  private void mockImplementingProperty(BaseDeclaration declaration, Class<?> type) {
    ImplementingTypeModelProperty property = type != null ? new ImplementingTypeModelProperty(type) : null;
    when(declaration.getModelProperty(ImplementingTypeModelProperty.class)).thenReturn(Optional.ofNullable(property));
  }

  public static class TestNameAwareConfig {

    @RefName
    private String name;

    public String getName() {
      return name;
    }
  }

  public static class TestMultipleNameAwareConfig {

    @RefName
    private String name;

    @RefName
    private String redundantName;
  }

  public static class TestIllegalNameAwareConfig {

    @RefName
    private Apple name;
  }

  public static class TestNameAwareConnectionProvider implements ConnectionProvider<Object> {

    @RefName
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

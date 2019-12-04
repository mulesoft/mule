/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.dsl.model;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newListValue;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newObjectValue;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newParameterGroup;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getId;
import static org.mule.runtime.internal.dsl.DslConstants.VALUE_ATTRIBUTE_NAME;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.app.declaration.api.ConfigurationElementDeclaration;
import org.mule.runtime.app.declaration.api.ConnectionElementDeclaration;
import org.mule.runtime.app.declaration.api.ElementDeclaration;
import org.mule.runtime.app.declaration.api.OperationElementDeclaration;
import org.mule.runtime.app.declaration.api.SourceElementDeclaration;
import org.mule.runtime.app.declaration.api.TopLevelParameterDeclaration;
import org.mule.runtime.app.declaration.api.fluent.ElementDeclarer;
import org.mule.runtime.app.declaration.api.fluent.ParameterObjectValue;
import org.mule.runtime.config.api.dsl.model.DslElementModel;
import org.mule.runtime.config.api.dsl.model.DslElementModelFactory;

import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DeclarationElementModelFactoryTestCase extends AbstractDslModelTestCase {

  @Test
  public void testConfigDeclarationToElement() {

    ElementDeclarer ext = ElementDeclarer.forExtension(EXTENSION_NAME);
    ConfigurationElementDeclaration declaration = ext.newConfiguration(CONFIGURATION_NAME)
        .withRefName("sample")
        .withParameterGroup(newParameterGroup()
            .withParameter(CONTENT_NAME, CONTENT_VALUE)
            .withParameter(BEHAVIOUR_NAME, BEHAVIOUR_VALUE)
            .withParameter(LIST_NAME, newListValue().withValue(ITEM_VALUE).build())
            .getDeclaration())
        .withConnection(ext.newConnection(CONNECTION_PROVIDER_NAME)
            .withParameterGroup(newParameterGroup()
                .withParameter(CONTENT_NAME, CONTENT_VALUE)
                .withParameter(BEHAVIOUR_NAME, BEHAVIOUR_VALUE)
                .withParameter(LIST_NAME, newListValue().withValue(ITEM_VALUE).build())
                .getDeclaration())
            .getDeclaration())
        .getDeclaration();

    DslElementModel<ConfigurationModel> configElement = create(declaration);
    assertThat(configElement.getModel(), is(configuration));
    assertThat(configElement.getContainedElements().size(), is(4));

    assertThat(configElement.findElement(BEHAVIOUR_NAME).isPresent(), is(true));
    assertBehaviourParameter(configElement);

    assertThat(configElement.findElement(CONTENT_NAME).isPresent(), is(true));
    assertContentParameter(configElement.getContainedElements().get(1));

    assertThat(configElement.findElement(LIST_NAME).isPresent(), is(true));
    assertListParameter(configElement.getContainedElements().get(3));


    assertThat(configElement.findElement(CONNECTION_PROVIDER_NAME).isPresent(), is(true));
    DslElementModel connectionElement = configElement.getContainedElements().get(0);
    assertThat(connectionElement.getContainedElements().size(), is(3));
    assertBehaviourParameter(connectionElement);
    assertContentParameter((DslElementModel) connectionElement.findElement(CONTENT_NAME).get());
    assertListParameter((DslElementModel) connectionElement.findElement(LIST_NAME).get());
  }

  private void assertContentParameter(DslElementModel<?> contentModel) {
    assertThat(contentModel.getConfiguration().get().getValue().get(), is(CONTENT_VALUE));
  }

  private void assertBehaviourParameter(DslElementModel<?> elementModel) {
    assertThat(elementModel.getConfiguration().get().getParameters().get(BEHAVIOUR_NAME), is(BEHAVIOUR_VALUE));
  }

  private void assertListParameter(DslElementModel<?> listModel) {
    assertThat(listModel.getContainedElements().size(), is(1));
    assertThat(listModel.getContainedElements().get(0).getDsl().getElementName(), is(ITEM_NAME));

    DslElementModel<Object> itemModel = listModel.getContainedElements().get(0);
    assertThat(itemModel.getContainedElements().get(0).getDsl().getAttributeName(), is(VALUE_ATTRIBUTE_NAME));
    assertThat(itemModel.getContainedElements().get(0).getValue().get(), is(ITEM_VALUE));
  }

  @Test
  public void testOperationDeclarationToElement() {

    ElementDeclarer ext = ElementDeclarer.forExtension(EXTENSION_NAME);
    OperationElementDeclaration declaration = ext.newOperation(OPERATION_NAME)
        .withConfig(CONFIGURATION_NAME)
        .withParameterGroup(newParameterGroup()
            .withParameter(CONTENT_NAME, CONTENT_VALUE)
            .withParameter(BEHAVIOUR_NAME, BEHAVIOUR_VALUE)
            .getDeclaration())
        .getDeclaration();

    DslElementModel<OperationModel> element = create(declaration);
    assertThat(element.getModel(), is(operation));
    assertThat(element.getContainedElements().size(), is(2));
    assertThat(element.findElement(BEHAVIOUR_NAME).isPresent(), is(true));
    assertThat(element.findElement(CONTENT_NAME).get().getConfiguration().get().getValue().get(), is(CONTENT_VALUE));
    assertThat(element.getConfiguration().get().getParameters().get(BEHAVIOUR_NAME), is(BEHAVIOUR_VALUE));
  }

  @Test
  public void testSourceDeclarationToElement() {

    ElementDeclarer ext = ElementDeclarer.forExtension(EXTENSION_NAME);
    SourceElementDeclaration declaration = ext.newSource(SOURCE_NAME)
        .withConfig(CONFIGURATION_NAME)
        .withParameterGroup(newParameterGroup()
            .withParameter(BEHAVIOUR_NAME, BEHAVIOUR_VALUE)
            .withParameter(CONTENT_NAME, CONTENT_VALUE)
            .getDeclaration())
        .getDeclaration();

    DslElementModel<SourceModel> element = create(declaration);
    assertThat(element.getModel(), is(source));
    assertThat(element.getContainedElements().size(), is(2));
    assertThat(element.findElement(BEHAVIOUR_NAME).isPresent(), is(true));
    assertThat(element.findElement(CONTENT_NAME).get().getConfiguration().get().getValue().get(), is(CONTENT_VALUE));
    assertThat(element.getConfiguration().get().getParameters().get(BEHAVIOUR_NAME), is(BEHAVIOUR_VALUE));
  }

  @Test
  public void testGlobalParameterDeclarationToElement() {

    ElementDeclarer ext = ElementDeclarer.forExtension(EXTENSION_NAME);
    final ParameterObjectValue.Builder value = newObjectValue()
        .withParameter(BEHAVIOUR_NAME, BEHAVIOUR_VALUE)
        .withParameter(CONTENT_NAME, CONTENT_VALUE);
    getId(complexType).ifPresent(value::ofType);

    TopLevelParameterDeclaration declaration = ext.newGlobalParameter(SOURCE_NAME)
        .withRefName("globalParameter")
        .withValue(value
            .build())
        .getDeclaration();

    DslElementModel<MetadataType> element = create(declaration);
    assertThat(element.getModel(), is(complexType));
    assertThat(element.getContainedElements().size(), is(2));
    assertThat(element.findElement(BEHAVIOUR_NAME).isPresent(), is(true));
    assertThat(element.findElement("myCamelCaseName").get()
        .getValue().get(), is(CONTENT_VALUE));
    assertThat(element.getConfiguration().get().getParameters().get(BEHAVIOUR_NAME), is(BEHAVIOUR_VALUE));
  }

  @Test
  public void testConfigNoConnectionNoParams() {

    ConfigurationModel emptyConfig = mock(ConfigurationModel.class, withSettings().lenient());
    when(emptyConfig.getName()).thenReturn(CONFIGURATION_NAME);
    when(emptyConfig.getParameterGroupModels()).thenReturn(emptyList());
    when(emptyConfig.getOperationModels()).thenReturn(emptyList());
    when(emptyConfig.getSourceModels()).thenReturn(emptyList());
    when(emptyConfig.getConnectionProviders()).thenReturn(emptyList());

    ExtensionModel extensionModel = mock(ExtensionModel.class, withSettings().lenient());
    initializeExtensionMock(extensionModel);
    when(extensionModel.getConfigurationModels()).thenReturn(asList(emptyConfig));

    ConfigurationElementDeclaration declaration =
        ElementDeclarer.forExtension(EXTENSION_NAME).newConfiguration(CONFIGURATION_NAME)
            .withRefName("sample")
            .getDeclaration();

    DslElementModel<ConfigurationModel> element = create(declaration);
    assertThat(element.getModel(), is(configuration));
    assertThat(element.getContainedElements().isEmpty(), is(true));
  }

  @Test
  public void testConnectionNoParams() {
    ConnectionProviderModel emptyConnection = mock(ConnectionProviderModel.class, withSettings().lenient());
    when(connectionProvider.getName()).thenReturn(CONNECTION_PROVIDER_NAME);
    when(connectionProvider.getParameterGroupModels()).thenReturn(emptyList());

    ExtensionModel extensionModel = mock(ExtensionModel.class, withSettings().lenient());
    initializeExtensionMock(extensionModel);
    when(extensionModel.getConnectionProviders()).thenReturn(asList(emptyConnection));

    ConnectionElementDeclaration declaration =
        ElementDeclarer.forExtension(EXTENSION_NAME).newConnection(CONNECTION_PROVIDER_NAME)
            .getDeclaration();

    DslElementModel<ConnectionProviderModel> element = create(declaration);
    assertThat(element.getModel(), is(connectionProvider));
    assertThat(element.getContainedElements().isEmpty(), is(true));
  }

  @Test
  public void testConfigNoParams() {

    ConfigurationElementDeclaration declaration = ElementDeclarer.forExtension(EXTENSION_NAME)
        .newConfiguration(CONFIGURATION_NAME)
        .withRefName("sample")
        .getDeclaration();

    DslElementModel<ConfigurationModel> element = create(declaration);
    assertThat(element.getModel(), is(configuration));
    assertThat(element.getContainedElements().isEmpty(), is(true));
  }

  protected <T> DslElementModel<T> create(ElementDeclaration declaration) {
    Optional<DslElementModel<T>> elementModel = DslElementModelFactory.getDefault(dslContext).create(declaration);
    if (!elementModel.isPresent()) {
      fail("Could not create element model for declared element: " + declaration.getName());
    }
    return elementModel.get();
  }
}

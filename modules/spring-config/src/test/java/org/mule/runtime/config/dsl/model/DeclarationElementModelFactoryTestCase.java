/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.dsl.model;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Optional.empty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newListValue;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newObjectValue;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newParameterGroup;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.BEHAVIOUR;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.CONTENT;
import static org.mule.runtime.api.util.ExtensionModelTestUtils.visitableMock;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getId;
import static org.mule.runtime.internal.dsl.DslConstants.VALUE_ATTRIBUTE_NAME;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.builder.ObjectTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.app.declaration.api.ConfigurationElementDeclaration;
import org.mule.runtime.app.declaration.api.ElementDeclaration;
import org.mule.runtime.app.declaration.api.OperationElementDeclaration;
import org.mule.runtime.app.declaration.api.SourceElementDeclaration;
import org.mule.runtime.app.declaration.api.TopLevelParameterDeclaration;
import org.mule.runtime.app.declaration.api.fluent.ElementDeclarer;
import org.mule.runtime.app.declaration.api.fluent.ParameterObjectValue;
import org.mule.runtime.config.api.dsl.model.DslElementModel;
import org.mule.runtime.config.api.dsl.model.DslElementModelFactory;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.ParameterDslConfiguration;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.type.TypeCatalog;
import org.mule.runtime.dsl.api.component.config.ComponentConfiguration;
import org.mule.runtime.extension.api.annotation.dsl.xml.TypeDsl;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DeclarationElementModelFactoryTestCase {

  private static final String NAMESPACE = "mockns";
  private static final String NAMESPACE_URI = "http://www.mulesoft.org/schema/mule/mockns";
  private static final String SCHEMA_LOCATION = "http://www.mulesoft.org/schema/mule/mockns/current/mule-mockns.xsd";
  private static final String CONTENT_NAME = "myCamelCaseName";
  private static final String BEHAVIOUR_NAME = "otherName";
  private static final String LIST_NAME = "listName";
  private static final String EXTENSION_NAME = "extension";
  private static final String OPERATION_NAME = "mockOperation";
  private static final String SOURCE_NAME = "source";
  private static final String CONFIGURATION_NAME = "configuration";
  private static final String CONNECTION_PROVIDER_NAME = "connection";
  private static final BaseTypeBuilder TYPE_BUILDER = BaseTypeBuilder.create(JAVA);

  @Mock
  private ExtensionModel extension;

  @Mock
  private ConfigurationModel configuration;

  @Mock
  private OperationModel operation;

  @Mock
  private ConnectionProviderModel connectionProvider;

  @Mock
  private ParameterModel contentParameter;

  @Mock
  private ParameterModel behaviourParameter;

  @Mock
  private ParameterModel listParameter;

  @Mock
  private ParameterGroupModel parameterGroupModel;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private SourceModel source;

  @Mock
  private DslResolvingContext dslContext;

  @Mock
  private TypeCatalog typeCatalog;

  private ObjectType complexType;

  private ClassTypeLoader TYPE_LOADER = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

  @Before
  public void before() {
    initMocks(this);

    initializeExtensionMock(extension);

    when(configuration.getName()).thenReturn(CONFIGURATION_NAME);
    when(configuration.getParameterGroupModels()).thenReturn(asList(parameterGroupModel));
    when(configuration.getOperationModels()).thenReturn(asList(operation));
    when(configuration.getSourceModels()).thenReturn(asList(source));
    when(configuration.getConnectionProviders()).thenReturn(asList(connectionProvider));

    when(behaviourParameter.getName()).thenReturn(BEHAVIOUR_NAME);
    when(behaviourParameter.getExpressionSupport()).thenReturn(ExpressionSupport.NOT_SUPPORTED);
    when(behaviourParameter.getModelProperty(any())).thenReturn(empty());
    when(behaviourParameter.getDslConfiguration()).thenReturn(ParameterDslConfiguration.getDefaultInstance());
    when(behaviourParameter.getLayoutModel()).thenReturn(empty());
    when(behaviourParameter.getRole()).thenReturn(BEHAVIOUR);
    when(behaviourParameter.getType()).thenReturn(TYPE_LOADER.load(String.class));

    when(listParameter.getName()).thenReturn(LIST_NAME);
    when(listParameter.getExpressionSupport()).thenReturn(ExpressionSupport.NOT_SUPPORTED);
    when(listParameter.getModelProperty(any())).thenReturn(empty());
    when(listParameter.getDslConfiguration()).thenReturn(ParameterDslConfiguration.getDefaultInstance());
    when(listParameter.getLayoutModel()).thenReturn(empty());
    when(listParameter.getRole()).thenReturn(BEHAVIOUR);
    when(listParameter.getType()).thenReturn(TYPE_BUILDER.arrayType().of(TYPE_LOADER.load(String.class)).build());

    when(contentParameter.getName()).thenReturn(CONTENT_NAME);
    when(contentParameter.getExpressionSupport()).thenReturn(ExpressionSupport.SUPPORTED);
    when(contentParameter.getModelProperty(any())).thenReturn(empty());
    when(contentParameter.getDslConfiguration()).thenReturn(ParameterDslConfiguration.getDefaultInstance());
    when(contentParameter.getLayoutModel()).thenReturn(empty());
    when(contentParameter.getRole()).thenReturn(CONTENT);

    ObjectTypeBuilder type = TYPE_BUILDER.objectType();
    type.addField().key("field").value(TYPE_LOADER.load(String.class)).build();
    when(contentParameter.getType()).thenReturn(type.build());

    List<ParameterModel> parameterModels = asList(contentParameter, behaviourParameter, listParameter);
    when(parameterGroupModel.getName()).thenReturn(DEFAULT_GROUP_NAME);
    when(parameterGroupModel.isShowInDsl()).thenReturn(false);
    when(parameterGroupModel.getParameterModels()).thenReturn(parameterModels);

    when(source.getName()).thenReturn(SOURCE_NAME);
    when(source.getParameterGroupModels()).thenReturn(asList(parameterGroupModel));
    when(source.getSuccessCallback()).thenReturn(empty());
    when(source.getErrorCallback()).thenReturn(empty());
    when(operation.getName()).thenReturn(OPERATION_NAME);
    when(operation.getParameterGroupModels()).thenReturn(asList(parameterGroupModel));
    visitableMock(operation, source);

    when(connectionProvider.getName()).thenReturn(CONNECTION_PROVIDER_NAME);
    when(connectionProvider.getParameterGroupModels()).thenReturn(asList(parameterGroupModel));

    when(typeCatalog.getSubTypes(any())).thenReturn(emptySet());
    when(typeCatalog.getSuperTypes(any())).thenReturn(emptySet());
    when(typeCatalog.getAllBaseTypes()).thenReturn(emptySet());
    when(typeCatalog.getAllSubTypes()).thenReturn(emptySet());
    when(typeCatalog.getTypes()).thenReturn(emptySet());

    complexType = (ObjectType) TYPE_LOADER.load(ComplexTypePojo.class);
    when(typeCatalog.getType(any())).thenReturn(Optional.of(complexType));
    when(typeCatalog.containsBaseType(any())).thenReturn(false);

    when(dslContext.getExtension(any())).thenReturn(Optional.of(extension));
    when(dslContext.getExtensions()).thenReturn(singleton(extension));
    when(dslContext.getTypeCatalog()).thenReturn(typeCatalog);

    Stream.of(configuration, operation, connectionProvider, source)
        .forEach(model -> when(model.getAllParameterModels()).thenReturn(parameterModels));
  }

  private void initializeExtensionMock(ExtensionModel extension) {
    when(extension.getName()).thenReturn(EXTENSION_NAME);
    when(extension.getXmlDslModel()).thenReturn(XmlDslModel.builder()
        .setXsdFileName("mule-mockns.xsd")
        .setPrefix(NAMESPACE)
        .setNamespace(NAMESPACE_URI)
        .setSchemaLocation(SCHEMA_LOCATION)
        .setSchemaVersion("4.0")
        .build());
    when(extension.getSubTypes()).thenReturn(emptySet());
    when(extension.getImportedTypes()).thenReturn(emptySet());
    when(extension.getXmlDslModel()).thenReturn(XmlDslModel.builder()
        .setXsdFileName(EMPTY)
        .setPrefix(NAMESPACE)
        .setNamespace(NAMESPACE_URI)
        .setSchemaLocation(SCHEMA_LOCATION)
        .setSchemaVersion(EMPTY)
        .build());

    when(extension.getConfigurationModels()).thenReturn(asList(configuration));
    when(extension.getOperationModels()).thenReturn(asList(operation));
    when(extension.getSourceModels()).thenReturn(asList(source));
    when(extension.getConnectionProviders()).thenReturn(asList(connectionProvider));
  }

  protected <T> DslElementModel<T> create(ElementDeclaration declaration) {
    Optional<DslElementModel<T>> elementModel = DslElementModelFactory.getDefault(dslContext).create(declaration);
    if (!elementModel.isPresent()) {
      fail("Could not create element model for declared element: " + declaration.getName());
    }
    return elementModel.get();
  }


  @Test
  public void testConfigDeclarationToElement() {

    ElementDeclarer ext = ElementDeclarer.forExtension(EXTENSION_NAME);
    ConfigurationElementDeclaration declaration = ext.newConfiguration(CONFIGURATION_NAME)
        .withRefName("sample")
        .withConnection(ext.newConnection(CONNECTION_PROVIDER_NAME)
            .withParameterGroup(newParameterGroup()
                .withParameter(CONTENT_NAME, "#[{field: value}]")
                .withParameter(BEHAVIOUR_NAME, "additional")
                .withParameter(LIST_NAME, newListValue().withValue("additional").build())
                .getDeclaration())
            .getDeclaration())
        .getDeclaration();

    DslElementModel<ConfigurationModel> element = create(declaration);
    assertThat(element.getModel(), is(configuration));
    assertThat(element.getContainedElements().size(), is(1));
    DslElementModel connectionElement = element.getContainedElements().get(0);
    assertThat(connectionElement.getContainedElements().size(), is(3));
    assertThat(element.findElement(LIST_NAME).isPresent(), is(true));

    DslElementModel<Object> listModel = element.findElement(LIST_NAME).get();
    assertThat(listModel.getContainedElements().size(), is(1));
    assertThat(listModel.getContainedElements().get(0).getDsl().getElementName(), is("list-name-item"));
    DslElementModel<Object> itemModel = listModel.getContainedElements().get(0);
    assertThat(itemModel.getContainedElements().get(0).getDsl().getAttributeName(), is(VALUE_ATTRIBUTE_NAME));
    assertThat(itemModel.getContainedElements().get(0).getValue().get(), is("additional"));

    assertThat(element.findElement(CONNECTION_PROVIDER_NAME).isPresent(), is(true));
    assertThat(element.findElement(CONTENT_NAME).get().getConfiguration().get().getValue().get(), is("#[{field: value}]"));
    assertThat(((ComponentConfiguration) connectionElement.getConfiguration().get())
        .getParameters().get(BEHAVIOUR_NAME), is("additional"));

  }

  @Test
  public void testOperationDeclarationToElement() {

    ElementDeclarer ext = ElementDeclarer.forExtension(EXTENSION_NAME);
    OperationElementDeclaration declaration = ext.newOperation(OPERATION_NAME)
        .withConfig(CONFIGURATION_NAME)
        .withParameterGroup(newParameterGroup()
            .withParameter(CONTENT_NAME, "#[{field: value}]")
            .withParameter(BEHAVIOUR_NAME, "additional")
            .getDeclaration())
        .getDeclaration();

    DslElementModel<OperationModel> element = create(declaration);
    assertThat(element.getModel(), is(operation));
    assertThat(element.getContainedElements().size(), is(2));
    assertThat(element.findElement(BEHAVIOUR_NAME).isPresent(), is(true));
    assertThat(element.findElement(CONTENT_NAME).get().getConfiguration().get().getValue().get(), is("#[{field: value}]"));
    assertThat(element.getConfiguration().get().getParameters().get(BEHAVIOUR_NAME), is("additional"));
  }

  @Test
  public void testSourceDeclarationToElement() {

    ElementDeclarer ext = ElementDeclarer.forExtension(EXTENSION_NAME);
    SourceElementDeclaration declaration = ext.newSource(SOURCE_NAME)
        .withConfig(CONFIGURATION_NAME)
        .withParameterGroup(newParameterGroup()
            .withParameter(BEHAVIOUR_NAME, "additional")
            .withParameter(CONTENT_NAME, "#[{field: value}]")
            .getDeclaration())
        .getDeclaration();

    DslElementModel<SourceModel> element = create(declaration);
    assertThat(element.getModel(), is(source));
    assertThat(element.getContainedElements().size(), is(2));
    assertThat(element.findElement(BEHAVIOUR_NAME).isPresent(), is(true));
    assertThat(element.findElement(CONTENT_NAME).get().getConfiguration().get().getValue().get(), is("#[{field: value}]"));
    assertThat(element.getConfiguration().get().getParameters().get(BEHAVIOUR_NAME), is("additional"));
  }

  @Test
  public void testGlobalParameterDeclarationToElement() {

    ElementDeclarer ext = ElementDeclarer.forExtension(EXTENSION_NAME);
    final ParameterObjectValue.Builder value = newObjectValue()
        .withParameter(BEHAVIOUR_NAME, "additional")
        .withParameter(CONTENT_NAME, "#[{field: value}]");
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
        .getValue().get(), is("#[{field: value}]"));
    assertThat(element.getConfiguration().get().getParameters().get(BEHAVIOUR_NAME), is("additional"));
  }

  @Test
  public void testConfigNoConnectionNoParams() {

    ConfigurationModel emptyConfig = mock(ConfigurationModel.class);
    when(emptyConfig.getName()).thenReturn(CONFIGURATION_NAME);
    when(emptyConfig.getParameterGroupModels()).thenReturn(emptyList());
    when(emptyConfig.getOperationModels()).thenReturn(emptyList());
    when(emptyConfig.getSourceModels()).thenReturn(emptyList());
    when(emptyConfig.getConnectionProviders()).thenReturn(emptyList());

    ExtensionModel extensionModel = mock(ExtensionModel.class);
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
  public void testConfigNoParams() {

    ConfigurationElementDeclaration declaration = ElementDeclarer.forExtension(EXTENSION_NAME)
        .newConfiguration(CONFIGURATION_NAME)
        .withRefName("sample")
        .getDeclaration();

    DslElementModel<ConfigurationModel> element = create(declaration);
    assertThat(element.getModel(), is(configuration));
    assertThat(element.getContainedElements().isEmpty(), is(true));
  }


  @TypeDsl(allowTopLevelDefinition = true)
  public static class ComplexTypePojo {

    @Parameter
    private String otherName;

    @Parameter
    @Content
    private String myCamelCaseName;

    public String getOtherName() {
      return otherName;
    }

    public void setOtherName(String otherName) {
      this.otherName = otherName;
    }

    public String getMyCamelCaseName() {
      return myCamelCaseName;
    }

    public void setMyCamelCaseName(String myCamelCaseName) {
      this.myCamelCaseName = myCamelCaseName;
    }
  }
}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.dsl.model;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Optional.empty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.BEHAVIOUR;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.CONTENT;
import static org.mule.runtime.api.util.ExtensionModelTestUtils.visitableMock;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.builder.ObjectTypeBuilder;
import org.mule.metadata.api.model.ObjectType;
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
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.dsl.xml.TypeDsl;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.Before;
import org.mockito.Mock;
import org.mule.runtime.extension.api.property.RequiredForMetadataModelProperty;

public abstract class AbstractDslModelTestCase {

  protected static final String NAMESPACE = "mockns";
  protected static final String NAMESPACE_URI = "http://www.mulesoft.org/schema/mule/mockns";
  protected static final String SCHEMA_LOCATION = "http://www.mulesoft.org/schema/mule/mockns/current/mule-mockns.xsd";
  protected static final String CONTENT_NAME = "myCamelCaseName";
  protected static final String BEHAVIOUR_NAME = "otherName";
  protected static final String LIST_NAME = "listName";
  protected static final String EXTENSION_NAME = "extension";
  protected static final String OPERATION_NAME = "mockOperation";
  protected static final String SOURCE_NAME = "source";
  protected static final String CONFIGURATION_NAME = "configuration";
  protected static final String CONNECTION_PROVIDER_NAME = "connection";
  protected static final BaseTypeBuilder TYPE_BUILDER = BaseTypeBuilder.create(JAVA);
  protected static final String BEHAVIOUR_VALUE = "additional";
  protected static final String CONTENT_VALUE = "#[{field: value}]";
  protected static final String ITEM_VALUE = "itemValue";
  protected static final String ITEM_NAME = "list-name-item";

  @Mock
  protected ExtensionModel mockExtension;

  @Mock
  protected ConfigurationModel configuration;

  @Mock
  protected OperationModel operation;

  @Mock
  protected ConnectionProviderModel connectionProvider;

  @Mock
  protected ParameterModel contentParameter;

  @Mock
  protected ParameterModel behaviourParameter;

  @Mock
  protected ParameterModel listParameter;

  @Mock
  protected ParameterGroupModel parameterGroupModel;

  @Mock(answer = RETURNS_DEEP_STUBS)
  protected SourceModel source;

  @Mock
  protected DslResolvingContext dslContext;

  @Mock
  protected TypeCatalog typeCatalog;

  protected ObjectType complexType;

  protected ClassTypeLoader TYPE_LOADER = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();
  protected List<ParameterModel> defaultGroupParameterModels;

  @Before
  public void before() {
    initMocks(this);

    initializeExtensionMock(mockExtension);

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

    this.defaultGroupParameterModels = asList(contentParameter, behaviourParameter, listParameter);
    when(parameterGroupModel.getName()).thenReturn(DEFAULT_GROUP_NAME);
    when(parameterGroupModel.isShowInDsl()).thenReturn(false);
    when(parameterGroupModel.getParameterModels()).thenReturn(defaultGroupParameterModels);
    when(parameterGroupModel.getParameter(anyString()))
        .then(invocation -> {
          String paramName = invocation.getArgumentAt(0, String.class);
          switch (paramName) {
            case CONTENT_NAME:
              return Optional.of(contentParameter);
            case LIST_NAME:
              return Optional.of(listParameter);
            case BEHAVIOUR_NAME:
              return Optional.of(behaviourParameter);
          }
          return Optional.empty();
        });

    List<String> parameters = new ArrayList<>();
    parameters.add(BEHAVIOUR_NAME);
    RequiredForMetadataModelProperty requiredForMetadataModelProperty = new RequiredForMetadataModelProperty(parameters);

    when(connectionProvider.getName()).thenReturn(CONNECTION_PROVIDER_NAME);
    when(connectionProvider.getParameterGroupModels()).thenReturn(asList(parameterGroupModel));

    when(configuration.getName()).thenReturn(CONFIGURATION_NAME);
    when(configuration.getParameterGroupModels()).thenReturn(asList(parameterGroupModel));
    when(configuration.getOperationModels()).thenReturn(asList(operation));
    when(configuration.getSourceModels()).thenReturn(asList(source));
    when(configuration.getConnectionProviders()).thenReturn(asList(connectionProvider));

    when(configuration.getModelProperty(RequiredForMetadataModelProperty.class))
        .thenReturn(Optional.of(requiredForMetadataModelProperty));
    when(connectionProvider.getModelProperty(RequiredForMetadataModelProperty.class))
        .thenReturn(Optional.of(requiredForMetadataModelProperty));

    when(source.getName()).thenReturn(SOURCE_NAME);
    when(source.getParameterGroupModels()).thenReturn(asList(parameterGroupModel));
    when(source.getSuccessCallback()).thenReturn(empty());
    when(source.getErrorCallback()).thenReturn(empty());
    when(operation.getName()).thenReturn(OPERATION_NAME);
    when(operation.getParameterGroupModels()).thenReturn(asList(parameterGroupModel));
    visitableMock(operation, source);

    when(typeCatalog.getSubTypes(any())).thenReturn(emptySet());
    when(typeCatalog.getSuperTypes(any())).thenReturn(emptySet());
    when(typeCatalog.getAllBaseTypes()).thenReturn(emptySet());
    when(typeCatalog.getAllSubTypes()).thenReturn(emptySet());
    when(typeCatalog.getTypes()).thenReturn(emptySet());

    complexType = (ObjectType) TYPE_LOADER.load(ComplexTypePojo.class);
    when(typeCatalog.getType(any())).thenReturn(Optional.of(complexType));
    when(typeCatalog.containsBaseType(any())).thenReturn(false);

    when(dslContext.getExtension(any())).thenReturn(Optional.of(mockExtension));
    when(dslContext.getExtensions()).thenReturn(singleton(mockExtension));
    when(dslContext.getTypeCatalog()).thenReturn(typeCatalog);

    Stream.of(configuration, operation, connectionProvider, source)
        .forEach(model -> when(model.getAllParameterModels()).thenReturn(defaultGroupParameterModels));
  }

  protected void initializeExtensionMock(ExtensionModel extension) {
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
    when(extension.getConfigurationModel(anyString())).thenReturn(Optional.of(configuration));
    when(extension.getOperationModels()).thenReturn(asList(operation));
    when(extension.getOperationModel(anyString())).thenReturn(Optional.of(operation));
    when(extension.getSourceModels()).thenReturn(asList(source));
    when(extension.getSourceModel(anyString())).thenReturn(Optional.of(source));
    when(extension.getConnectionProviders()).thenReturn(asList(connectionProvider));
    when(extension.getConnectionProviderModel(anyString())).thenReturn(Optional.of(connectionProvider));
  }

  @TypeDsl(allowTopLevelDefinition = true)
  @Alias("complexType")
  public static class ComplexTypePojo {

    @Parameter
    private String otherName;

    @Parameter
    @Content
    private String myCamelCaseName;

    @Parameter
    private List<Integer> numbers;

    public List<Integer> getNumbers() {
      return numbers;
    }

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

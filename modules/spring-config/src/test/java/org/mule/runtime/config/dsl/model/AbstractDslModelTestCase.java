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
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mockito.junit.MockitoJUnit.rule;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.ERROR_MAPPINGS;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.BEHAVIOUR;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.CONTENT;
import static org.mule.runtime.api.meta.model.stereotype.StereotypeModelBuilder.newStereotype;
import static org.mule.runtime.api.util.ExtensionModelTestUtils.visitableMock;
import static org.mule.runtime.extension.api.ExtensionConstants.ERROR_MAPPINGS_PARAMETER_NAME;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.CONFIG;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.CONNECTION;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.PROCESSOR;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.SOURCE;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.builder.ObjectTypeBuilder;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.ParameterDslConfiguration;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.api.meta.type.TypeCatalog;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.dsl.xml.TypeDsl;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyPart;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.error.ErrorMapping;
import org.mule.runtime.extension.api.property.MetadataKeyPartModelProperty;
import org.mule.runtime.extension.api.property.RequiredForMetadataModelProperty;
import org.mule.runtime.extension.api.property.TypeResolversInformationModelProperty;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.extension.api.stereotype.MuleStereotypes;
import org.mule.tck.junit4.AbstractMuleTestCase;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoRule;

public abstract class AbstractDslModelTestCase extends AbstractMuleTestCase {

  protected static final String NAMESPACE = "mockns";
  protected static final String NAMESPACE_URI = "http://www.mulesoft.org/schema/mule/mockns";
  protected static final String SCHEMA_LOCATION = "http://www.mulesoft.org/schema/mule/mockns/current/mule-mockns.xsd";
  protected static final String KEY_NAME = "metadataKeyParameter";
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
  protected static final String ANOTHER_OPERATION_NAME = "anotherMockOperation";
  protected static final String ANOTHER_CONTENT_NAME = "anotherMyCamelCaseName";

  @Rule
  public MockitoRule rule = rule().silent();

  @Mock(lenient = true)
  protected ExtensionModel mockExtension;

  @Mock(lenient = true)
  protected ConfigurationModel configuration;

  @Mock(lenient = true)
  protected OperationModel operation;

  @Mock(lenient = true)
  protected OperationModel anotherOperation;

  @Mock(lenient = true)
  protected ConnectionProviderModel connectionProvider;

  @Mock(lenient = true)
  protected ParameterModel contentParameter;

  @Mock(lenient = true)
  protected ParameterModel keyParameter;

  @Mock(lenient = true)
  protected ParameterModel anotherContentParameter;

  @Mock(lenient = true)
  protected ParameterModel nameParameter;

  @Mock(lenient = true)
  protected ParameterModel configRefParameter;

  @Mock(lenient = true)
  protected ParameterModel behaviourParameter;

  @Mock(lenient = true)
  protected ParameterModel listParameter;

  @Mock(lenient = true)
  protected ParameterGroupModel parameterGroupModel;

  @Mock(lenient = true)
  protected ParameterGroupModel anotherParameterGroupModel;

  @Mock(lenient = true)
  protected ParameterModel errorMappingsParameter;

  @Mock(lenient = true)
  protected ParameterGroupModel errorMappingsParameterGroup;

  @Mock(answer = RETURNS_DEEP_STUBS, lenient = true)
  protected SourceModel source;

  @Mock(lenient = true)
  protected DslResolvingContext dslContext;

  @Mock(lenient = true)
  protected TypeCatalog typeCatalog;

  protected ObjectType complexType;

  protected ClassTypeLoader TYPE_LOADER = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();
  protected List<ParameterModel> defaultGroupParameterModels;
  protected List<ParameterModel> anotherDefaultGroupParameterModels;

  @Before
  public void before() {
    initMocks(this);

    initializeExtensionMock(mockExtension);

    when(nameParameter.getName()).thenReturn("name");
    when(nameParameter.getExpressionSupport()).thenReturn(ExpressionSupport.NOT_SUPPORTED);
    when(nameParameter.getModelProperty(any())).thenReturn(empty());
    when(nameParameter.getDslConfiguration()).thenReturn(ParameterDslConfiguration.getDefaultInstance());
    when(nameParameter.getLayoutModel()).thenReturn(empty());
    when(nameParameter.getRole()).thenReturn(BEHAVIOUR);
    when(nameParameter.getType()).thenReturn(TYPE_LOADER.load(String.class));
    when(nameParameter.isComponentId()).thenReturn(true);

    when(configRefParameter.getName()).thenReturn("config-ref");
    when(configRefParameter.getExpressionSupport()).thenReturn(NOT_SUPPORTED);
    when(configRefParameter.getModelProperty(any())).thenReturn(empty());
    when(configRefParameter.getDslConfiguration()).thenReturn(ParameterDslConfiguration.getDefaultInstance());
    when(configRefParameter.getLayoutModel()).thenReturn(empty());
    when(configRefParameter.getRole()).thenReturn(BEHAVIOUR);
    when(configRefParameter.getType()).thenReturn(TYPE_LOADER.load(ConfigurationProvider.class));
    final List<StereotypeModel> configStereotypes = singletonList(
                                                                  newStereotype(CONFIGURATION_NAME, EXTENSION_NAME)
                                                                      .withParent(MuleStereotypes.CONFIG).build());
    when(configRefParameter.getAllowedStereotypes()).thenReturn(configStereotypes);

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

    when(keyParameter.getName()).thenReturn(KEY_NAME);
    when(keyParameter.getExpressionSupport()).thenReturn(NOT_SUPPORTED);
    when(keyParameter.getModelProperty(any())).thenReturn(empty());
    when(keyParameter.getDslConfiguration()).thenReturn(ParameterDslConfiguration.getDefaultInstance());
    when(keyParameter.getLayoutModel()).thenReturn(empty());
    when(keyParameter.getRole()).thenReturn(BEHAVIOUR);
    MetadataKeyPartModelProperty keyParameterMetadataKeyPartModelProperty = mock(MetadataKeyPartModelProperty.class);
    when(keyParameterMetadataKeyPartModelProperty.getName()).thenReturn("keyResolver");
    when(keyParameterMetadataKeyPartModelProperty.getOrder()).thenReturn(1);
    when(keyParameterMetadataKeyPartModelProperty.isPublic()).thenReturn(true);
    when(keyParameterMetadataKeyPartModelProperty.isProvidedByKeyResolver()).thenReturn(true);
    when(keyParameter.getModelProperty(MetadataKeyPartModelProperty.class))
        .thenReturn(of(keyParameterMetadataKeyPartModelProperty));

    when(anotherContentParameter.getName()).thenReturn(ANOTHER_CONTENT_NAME);
    when(anotherContentParameter.getExpressionSupport()).thenReturn(ExpressionSupport.SUPPORTED);
    when(anotherContentParameter.getModelProperty(any())).thenReturn(empty());
    when(anotherContentParameter.getDslConfiguration()).thenReturn(ParameterDslConfiguration.getDefaultInstance());
    when(anotherContentParameter.getLayoutModel()).thenReturn(empty());
    when(anotherContentParameter.getRole()).thenReturn(CONTENT);

    when(keyParameter.getType()).thenReturn(TYPE_LOADER.load(String.class));

    ObjectTypeBuilder type = TYPE_BUILDER.objectType();
    type.addField().key("field").value(TYPE_LOADER.load(String.class)).build();
    when(contentParameter.getType()).thenReturn(type.build());
    when(anotherContentParameter.getType()).thenReturn(type.build());

    this.defaultGroupParameterModels =
        asList(nameParameter, configRefParameter, contentParameter, behaviourParameter, listParameter, keyParameter);
    when(parameterGroupModel.getName()).thenReturn(DEFAULT_GROUP_NAME);
    when(parameterGroupModel.isShowInDsl()).thenReturn(false);
    when(parameterGroupModel.getParameterModels()).thenReturn(defaultGroupParameterModels);
    when(parameterGroupModel.getParameter(anyString()))
        .then(invocation -> {
          String paramName = invocation.getArgument(0);
          switch (paramName) {
            case KEY_NAME:
              return of(keyParameter);
            case CONTENT_NAME:
              return of(contentParameter);
            case LIST_NAME:
              return of(listParameter);
            case BEHAVIOUR_NAME:
              return of(behaviourParameter);
          }
          return Optional.empty();
        });

    this.anotherDefaultGroupParameterModels = asList(anotherContentParameter, listParameter, configRefParameter, keyParameter);
    when(anotherParameterGroupModel.getName()).thenReturn(DEFAULT_GROUP_NAME);
    when(anotherParameterGroupModel.isShowInDsl()).thenReturn(false);
    when(anotherParameterGroupModel.getParameterModels()).thenReturn(anotherDefaultGroupParameterModels);
    when(anotherParameterGroupModel.getParameter(anyString()))
        .then(invocation -> {
          String paramName = invocation.getArgument(0);
          switch (paramName) {
            case KEY_NAME:
              return of(keyParameter);
            case ANOTHER_CONTENT_NAME:
              return of(anotherContentParameter);
            case LIST_NAME:
              return of(listParameter);
          }
          return Optional.empty();
        });

    when(errorMappingsParameter.getName()).thenReturn(ERROR_MAPPINGS_PARAMETER_NAME);
    when(errorMappingsParameter.getExpressionSupport()).thenReturn(NOT_SUPPORTED);
    when(errorMappingsParameter.getModelProperty(any())).thenReturn(empty());
    when(errorMappingsParameter.getDslConfiguration()).thenReturn(ParameterDslConfiguration.getDefaultInstance());
    when(errorMappingsParameter.getLayoutModel()).thenReturn(empty());
    when(errorMappingsParameter.getRole()).thenReturn(BEHAVIOUR);
    when(errorMappingsParameter.getType()).thenReturn(BaseTypeBuilder.create(JAVA).arrayType()
        .of(TYPE_LOADER.load(ErrorMapping.class)).build());

    when(errorMappingsParameterGroup.getName()).thenReturn(ERROR_MAPPINGS);
    when(errorMappingsParameterGroup.isShowInDsl()).thenReturn(false);
    when(errorMappingsParameterGroup.getParameterModels()).thenReturn(asList(errorMappingsParameter));
    when(errorMappingsParameterGroup.getParameter(ERROR_MAPPINGS_PARAMETER_NAME)).thenReturn(of(errorMappingsParameter));

    List<String> parameters = new ArrayList<>();
    parameters.add(BEHAVIOUR_NAME);
    RequiredForMetadataModelProperty requiredForMetadataModelProperty = new RequiredForMetadataModelProperty(parameters);

    when(connectionProvider.getName()).thenReturn(CONNECTION_PROVIDER_NAME);
    when(connectionProvider.getParameterGroupModels()).thenReturn(asList(parameterGroupModel));
    when(connectionProvider.getStereotype()).thenReturn(CONNECTION);

    when(configuration.getName()).thenReturn(CONFIGURATION_NAME);
    when(configuration.getParameterGroupModels()).thenReturn(asList(parameterGroupModel));
    when(configuration.getOperationModels()).thenReturn(asList(operation));
    when(configuration.getSourceModels()).thenReturn(asList(source));
    when(configuration.getConnectionProviders()).thenReturn(asList(connectionProvider));
    when(configuration.getStereotype()).thenReturn(CONFIG);

    when(configuration.getModelProperty(RequiredForMetadataModelProperty.class))
        .thenReturn(of(requiredForMetadataModelProperty));
    when(connectionProvider.getModelProperty(RequiredForMetadataModelProperty.class))
        .thenReturn(of(requiredForMetadataModelProperty));

    when(source.getName()).thenReturn(SOURCE_NAME);
    when(source.getParameterGroupModels()).thenReturn(asList(parameterGroupModel));
    when(source.getSuccessCallback()).thenReturn(empty());
    when(source.getErrorCallback()).thenReturn(empty());
    when(source.getStereotype()).thenReturn(SOURCE);

    when(operation.getName()).thenReturn(OPERATION_NAME);
    when(operation.getParameterGroupModels()).thenReturn(asList(parameterGroupModel, errorMappingsParameterGroup));
    when(operation.getStereotype()).thenReturn(PROCESSOR);
    when(anotherOperation.getName()).thenReturn(ANOTHER_OPERATION_NAME);
    when(anotherOperation.getParameterGroupModels()).thenReturn(asList(anotherParameterGroupModel, errorMappingsParameterGroup));
    when(anotherOperation.getStereotype()).thenReturn(PROCESSOR);
    visitableMock(operation, source, anotherOperation);

    Map<String, String> parameterResolversNames = new HashMap<>();
    parameterResolversNames.put(LIST_NAME, LIST_NAME);
    parameterResolversNames.put(CONTENT_NAME, CONTENT_NAME);
    mockTypeResolversInformationModelProperty(operation, "category", "outputResolverName",
                                              "attributesResolverName", parameterResolversNames);
    mockTypeResolversInformationModelProperty(source, "category", "outputResolverName",
                                              "attributesResolverName", parameterResolversNames);
    Map<String, String> anotherParameterResolversNames = new HashMap<>();
    anotherParameterResolversNames.put(LIST_NAME, LIST_NAME);
    anotherParameterResolversNames.put(ANOTHER_CONTENT_NAME, ANOTHER_CONTENT_NAME);
    mockTypeResolversInformationModelProperty(anotherOperation, "category", "outputResolverName",
                                              "attributesResolverName", anotherParameterResolversNames);

    when(typeCatalog.getSubTypes(any())).thenReturn(emptySet());
    when(typeCatalog.getSuperTypes(any())).thenReturn(emptySet());
    when(typeCatalog.getAllBaseTypes()).thenReturn(emptySet());
    when(typeCatalog.getAllSubTypes()).thenReturn(emptySet());
    when(typeCatalog.getTypes()).thenReturn(emptySet());

    complexType = (ObjectType) TYPE_LOADER.load(ComplexTypePojo.class);
    when(typeCatalog.getType(any())).thenReturn(of(complexType));
    when(typeCatalog.containsBaseType(any())).thenReturn(false);

    when(dslContext.getExtension(any())).thenReturn(of(mockExtension));
    when(dslContext.getExtensions()).thenReturn(singleton(mockExtension));
    when(dslContext.getTypeCatalog()).thenReturn(typeCatalog);

    Stream.of(configuration, connectionProvider, source)
        .forEach(model -> when(model.getAllParameterModels()).thenReturn(defaultGroupParameterModels));
    when(operation.getAllParameterModels())
        .thenReturn(ImmutableList.<ParameterModel>builder()
            .addAll(defaultGroupParameterModels)
            .add(errorMappingsParameter).build());
    when(anotherOperation.getAllParameterModels())
        .thenReturn(ImmutableList.<ParameterModel>builder()
            .addAll(anotherDefaultGroupParameterModels)
            .add(errorMappingsParameter).build());
  }

  @After
  public void tearDown() {
    Mockito.framework().clearInlineMocks();
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
    when(extension.getConfigurationModels()).thenReturn(asList(configuration));
    when(extension.getConfigurationModel(anyString())).thenReturn(of(configuration));
    when(extension.getOperationModels()).thenReturn(asList(operation, anotherOperation));
    when(extension.getOperationModel(anyString())).then(invocation -> {
      String operationName = invocation.getArgument(0);
      switch (operationName) {
        case OPERATION_NAME:
          return of(operation);
        case ANOTHER_OPERATION_NAME:
          return of(anotherOperation);
      }
      return Optional.empty();
    });
    when(extension.getSourceModels()).thenReturn(asList(source));
    when(extension.getSourceModel(anyString())).thenReturn(of(source));
    when(extension.getConnectionProviders()).thenReturn(asList(connectionProvider));
    when(extension.getConnectionProviderModel(anyString())).thenReturn(of(connectionProvider));
  }

  protected void removeTypeResolversInformationModelPropertyfromMock(EnrichableModel model) {
    when(model.getModelProperty(TypeResolversInformationModelProperty.class)).thenReturn(empty());
  }

  protected void mockTypeResolversInformationModelPropertyWithOutputType(EnrichableModel model, String category,
                                                                         String outputResolverName) {
    mockTypeResolversInformationModelProperty(model, category, outputResolverName, null, null, null);
  }

  protected void mockTypeResolversInformationModelPropertyWithAttributeType(EnrichableModel model, String category,
                                                                            String attributesResolverName) {
    mockTypeResolversInformationModelProperty(model, category, null, attributesResolverName, null, null);
  }

  protected void mockTypeResolversInformationModelPropertyWithInputTypes(EnrichableModel model, String category,
                                                                         Map<String, String> parameterResolversNames) {
    mockTypeResolversInformationModelProperty(model, category, null, null, parameterResolversNames, null);
  }

  protected void mockTypeResolversInformationModelProperty(EnrichableModel model, String category, String outputResolverName,
                                                           String attributesResolverName,
                                                           Map<String, String> parameterResolversNames) {
    mockTypeResolversInformationModelProperty(model, category, outputResolverName, attributesResolverName,
                                              parameterResolversNames, null);

  }

  protected void mockTypeResolversInformationModelProperty(EnrichableModel model, String category, String outputResolverName,
                                                           String attributesResolverName,
                                                           Map<String, String> parameterResolversNames, String keysResolverName) {
    mockTypeResolversInformationModelProperty(model, category, outputResolverName, attributesResolverName,
                                              parameterResolversNames, keysResolverName, false);
  }

  protected void mockTypeResolversInformationModelProperty(EnrichableModel model, String category, String outputResolverName,
                                                           String attributesResolverName,
                                                           Map<String, String> parameterResolversNames, String keysResolverName,
                                                           boolean partialTypeKeyResolver) {
    when(model.getModelProperty(TypeResolversInformationModelProperty.class))
        .thenReturn(of(new TypeResolversInformationModelProperty(category,
                                                                 parameterResolversNames,
                                                                 outputResolverName,
                                                                 attributesResolverName,
                                                                 keysResolverName,
                                                                 false,
                                                                 false,
                                                                 partialTypeKeyResolver)));
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

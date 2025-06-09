/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.dsl.model;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mockito.junit.MockitoJUnit.rule;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.api.meta.Category.COMMUNITY;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.ExpressionSupport.SUPPORTED;
import static org.mule.runtime.api.meta.model.ComponentVisibility.PUBLIC;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.ERROR_MAPPINGS;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.BEHAVIOUR;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.CONTENT;
import static org.mule.runtime.api.meta.model.stereotype.StereotypeModelBuilder.newStereotype;
import static org.mule.runtime.extension.api.ExtensionConstants.ERROR_MAPPINGS_PARAMETER_NAME;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.CONFIG;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.CONNECTION;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.PROCESSOR;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.SOURCE;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.builder.ObjectTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.ParameterDslConfiguration;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.operation.ExecutionType;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterRole;
import org.mule.runtime.api.meta.model.parameter.ValueProviderModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.api.meta.type.TypeCatalog;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.dsl.xml.TypeDsl;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.error.ErrorMapping;
import org.mule.runtime.extension.api.model.ImmutableExtensionModel;
import org.mule.runtime.extension.api.model.operation.ImmutableOperationModel;
import org.mule.runtime.extension.api.model.parameter.ImmutableParameterGroupModel;
import org.mule.runtime.extension.api.model.parameter.ImmutableParameterModel;
import org.mule.runtime.extension.api.model.source.ImmutableSourceModel;
import org.mule.runtime.extension.api.property.MetadataKeyPartModelProperty;
import org.mule.runtime.extension.api.property.RequiredForMetadataModelProperty;
import org.mule.runtime.extension.api.property.TypeResolversInformationModelProperty;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.extension.api.stereotype.MuleStereotypes;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
  protected static final String CONFIG_REF_NAME = "config-ref";
  protected static final String NAME_PARAM_NAME = "name";
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

  protected ExtensionModel mockExtension;

  @Mock(lenient = true)
  protected ConfigurationModel configuration;

  protected OperationModel operation;

  protected OperationModel anotherOperation;

  @Mock(lenient = true)
  protected ConnectionProviderModel connectionProvider;

  protected ParameterModel contentParameter;

  protected ParameterModel keyParameter;

  protected ParameterModel anotherContentParameter;

  protected ParameterModel nameParameter;

  protected ParameterModel configRefParameter;

  protected ParameterModel behaviourParameter;

  protected ParameterModel listParameter;

  @Mock(lenient = true)
  protected ParameterGroupModel parameterGroupModel;

  @Mock(lenient = true)
  protected ParameterGroupModel anotherParameterGroupModel;

  @Mock(lenient = true)
  protected ParameterGroupModel configParameterGroupModel;

  protected ParameterModel errorMappingsParameter;

  protected ParameterGroupModel errorMappingsParameterGroup;

  protected SourceModel source;

  @Mock(lenient = true)
  protected DslResolvingContext dslContext;

  @Mock(lenient = true)
  protected TypeCatalog typeCatalog;

  protected ObjectType complexType;

  protected ClassTypeLoader TYPE_LOADER = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();
  protected List<ParameterModel> componentParameterModels;
  protected List<ParameterModel> anotherComponentParameterModels;
  protected List<ParameterModel> configParameterModels;

  @Before
  public void before() {
    initMocks(this);

    final MetadataType stringType = TYPE_LOADER.load(String.class);

    nameParameter =
        createParameterModel("name", true, stringType, null, NOT_SUPPORTED, BEHAVIOUR, null, emptyList(), emptySet());

    configRefParameter = createParameterModel("config-ref", false, TYPE_LOADER.load(ConfigurationProvider.class), null,
                                              NOT_SUPPORTED, BEHAVIOUR, null,
                                              singletonList(newStereotype(CONFIGURATION_NAME, EXTENSION_NAME)
                                                  .withParent(MuleStereotypes.CONFIG).build()),
                                              emptySet());

    behaviourParameter = createParameterModel(BEHAVIOUR_NAME, false, stringType, null,
                                              NOT_SUPPORTED, BEHAVIOUR, null,
                                              emptyList(), emptySet());

    listParameter = createParameterModel(LIST_NAME, false, TYPE_BUILDER.arrayType().of(stringType).build(), null,
                                         NOT_SUPPORTED, BEHAVIOUR, null,
                                         emptyList(), emptySet());

    ObjectTypeBuilder type = TYPE_BUILDER.objectType();
    type.addField().key("field").value(stringType).build();

    contentParameter = createParameterModel(CONTENT_NAME, false, type.build(), null,
                                            SUPPORTED, CONTENT, null,
                                            emptyList(), emptySet());

    MetadataKeyPartModelProperty keyParameterMetadataKeyPartModelProperty = new MetadataKeyPartModelProperty(1, true);

    keyParameter = createParameterModel(KEY_NAME, false, stringType, null,
                                        NOT_SUPPORTED, CONTENT, null,
                                        emptyList(), singleton(keyParameterMetadataKeyPartModelProperty));

    anotherContentParameter = createParameterModel(ANOTHER_CONTENT_NAME, false, type.build(), null,
                                                   SUPPORTED, CONTENT, null,
                                                   emptyList(), emptySet());

    this.componentParameterModels = asList(configRefParameter, contentParameter, behaviourParameter, listParameter, keyParameter);
    when(parameterGroupModel.getName()).thenReturn(DEFAULT_GROUP_NAME);
    when(parameterGroupModel.isShowInDsl()).thenReturn(false);
    when(parameterGroupModel.getParameterModels()).thenReturn(componentParameterModels);
    when(parameterGroupModel.getParameter(anyString()))
        .then(invocation -> {
          String paramName = invocation.getArgument(0);
          switch (paramName) {
            case CONFIG_REF_NAME:
              return of(configRefParameter);
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

    this.anotherComponentParameterModels = asList(configRefParameter, anotherContentParameter, listParameter, keyParameter);
    when(anotherParameterGroupModel.getName()).thenReturn(DEFAULT_GROUP_NAME);
    when(anotherParameterGroupModel.isShowInDsl()).thenReturn(false);
    when(anotherParameterGroupModel.getParameterModels()).thenReturn(anotherComponentParameterModels);
    when(anotherParameterGroupModel.getParameter(anyString()))
        .then(invocation -> {
          String paramName = invocation.getArgument(0);
          switch (paramName) {
            case CONFIG_REF_NAME:
              return of(configRefParameter);
            case KEY_NAME:
              return of(keyParameter);
            case ANOTHER_CONTENT_NAME:
              return of(anotherContentParameter);
            case LIST_NAME:
              return of(listParameter);
          }
          return Optional.empty();
        });

    this.configParameterModels = asList(nameParameter, contentParameter, behaviourParameter, listParameter, keyParameter);
    when(configParameterGroupModel.getName()).thenReturn(DEFAULT_GROUP_NAME);
    when(configParameterGroupModel.isShowInDsl()).thenReturn(false);
    when(configParameterGroupModel.getParameterModels()).thenReturn(configParameterModels);
    when(configParameterGroupModel.getParameter(anyString()))
        .then(invocation -> {
          String paramName = invocation.getArgument(0);
          switch (paramName) {
            case NAME_PARAM_NAME:
              return of(nameParameter);
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

    errorMappingsParameter = createParameterModel(ERROR_MAPPINGS_PARAMETER_NAME, false, BaseTypeBuilder.create(JAVA).arrayType()
        .of(TYPE_LOADER.load(ErrorMapping.class)).build(), null, NOT_SUPPORTED,
                                                  BEHAVIOUR, null, emptyList(), emptySet());

    errorMappingsParameterGroup = new ImmutableParameterGroupModel(ERROR_MAPPINGS, "", asList(errorMappingsParameter),
                                                                   emptyList(), false, null, null, emptySet());

    Map<String, String> parameterResolversNames = new HashMap<>();
    parameterResolversNames.put(LIST_NAME, LIST_NAME);
    parameterResolversNames.put(CONTENT_NAME, CONTENT_NAME);

    source =
        new ImmutableSourceModel(SOURCE_NAME, "", false, false, asList(parameterGroupModel), emptyList(), null, null, empty(),
                                 empty(), empty(), false, false, false, null, SOURCE, emptySet(), PUBLIC,
                                 singleton(createTypeResolversInformationModelProperty("category", "outputResolverName",
                                                                                       "attributesResolverName",
                                                                                       parameterResolversNames,
                                                                                       null, false)),
                                 emptySet(), null);

    operation = spy(createOperationModel(OPERATION_NAME, asList(parameterGroupModel, errorMappingsParameterGroup),
                                         singleton(createTypeResolversInformationModelProperty("category", "outputResolverName",
                                                                                               "attributesResolverName",
                                                                                               parameterResolversNames,
                                                                                               null, false))));
    Map<String, String> anotherParameterResolversNames = new HashMap<>();
    anotherParameterResolversNames.put(LIST_NAME, LIST_NAME);
    anotherParameterResolversNames.put(ANOTHER_CONTENT_NAME, ANOTHER_CONTENT_NAME);
    anotherOperation =
        spy(createOperationModel(ANOTHER_OPERATION_NAME, asList(anotherParameterGroupModel, errorMappingsParameterGroup),
                                 singleton(createTypeResolversInformationModelProperty("category", "outputResolverName",
                                                                                       "attributesResolverName",
                                                                                       anotherParameterResolversNames,
                                                                                       null, false))));

    List<String> parameters = new ArrayList<>();
    parameters.add(BEHAVIOUR_NAME);
    RequiredForMetadataModelProperty requiredForMetadataModelProperty = new RequiredForMetadataModelProperty(parameters);

    when(connectionProvider.getName()).thenReturn(CONNECTION_PROVIDER_NAME);
    when(connectionProvider.getParameterGroupModels()).thenReturn(asList(parameterGroupModel));
    when(connectionProvider.getStereotype()).thenReturn(CONNECTION);

    when(configuration.getName()).thenReturn(CONFIGURATION_NAME);
    when(configuration.getParameterGroupModels()).thenReturn(asList(configParameterGroupModel));
    when(configuration.getOperationModels()).thenReturn(asList(operation));
    when(configuration.getSourceModels()).thenReturn(asList(source));
    when(configuration.getConnectionProviders()).thenReturn(asList(connectionProvider));
    when(configuration.getStereotype()).thenReturn(CONFIG);

    when(configuration.getModelProperty(RequiredForMetadataModelProperty.class))
        .thenReturn(of(requiredForMetadataModelProperty));
    when(connectionProvider.getModelProperty(RequiredForMetadataModelProperty.class))
        .thenReturn(of(requiredForMetadataModelProperty));

    mockExtension = createExtension(EXTENSION_NAME, XmlDslModel.builder()
        .setXsdFileName("mule-mockns.xsd")
        .setPrefix(NAMESPACE)
        .setNamespace(NAMESPACE_URI)
        .setSchemaLocation(SCHEMA_LOCATION)
        .setSchemaVersion("4.0")
        .build(), asList(configuration), asList(connectionProvider));

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

    when(configuration.getAllParameterModels()).thenReturn(configParameterModels);
    when(connectionProvider.getAllParameterModels()).thenReturn(componentParameterModels);
  }

  protected ImmutableParameterModel createParameterModel(String paramName, boolean isComponentId, MetadataType type,
                                                         Object defaultValue, ExpressionSupport expressionSupport,
                                                         ParameterRole parameterRole, ValueProviderModel valueProviderModel,
                                                         List<StereotypeModel> allowedStereotypes,
                                                         Set<ModelProperty> modelProperties) {
    return new ImmutableParameterModel(paramName, "", type, false, false,
                                       false, isComponentId, expressionSupport, defaultValue,
                                       parameterRole, ParameterDslConfiguration.getDefaultInstance(), null, null,
                                       valueProviderModel,
                                       allowedStereotypes, modelProperties);
  }

  private ImmutableOperationModel createOperationModel(String operationName, List<ParameterGroupModel> paramGroups,
                                                       Set<ModelProperty> modelProperties) {
    return new ImmutableOperationModel(operationName, "",
                                       paramGroups,
                                       emptyList(), null, null, true, ExecutionType.BLOCKING, false, false, false, null,
                                       emptySet(), PROCESSOR, PUBLIC, modelProperties, emptySet());
  }

  @After
  public void tearDown() {
    Mockito.framework().clearInlineMocks();
  }

  protected ExtensionModel createExtension(String name, XmlDslModel xmlDslModel, List<ConfigurationModel> configs,
                                           List<ConnectionProviderModel> connectionProviders) {
    return new ImmutableExtensionModel(name,
                                       "",
                                       "1.0",
                                       "Mulesoft",
                                       COMMUNITY,
                                       asList(configuration),
                                       asList(operation, anotherOperation),
                                       asList(connectionProvider),
                                       asList(source),
                                       emptyList(),
                                       emptyList(),
                                       null,
                                       xmlDslModel,
                                       emptySet(),
                                       emptySet(),
                                       emptySet(),
                                       emptySet(),
                                       emptySet(),
                                       emptySet(),
                                       emptySet(),
                                       emptySet(),
                                       emptySet(),
                                       emptySet());
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
        .thenReturn(of(createTypeResolversInformationModelProperty(category, outputResolverName, attributesResolverName,
                                                                   parameterResolversNames,
                                                                   keysResolverName, partialTypeKeyResolver)));
  }

  protected TypeResolversInformationModelProperty createTypeResolversInformationModelProperty(String category,
                                                                                              String outputResolverName,
                                                                                              String attributesResolverName,
                                                                                              Map<String, String> parameterResolversNames,
                                                                                              String keysResolverName,
                                                                                              boolean partialTypeKeyResolver) {
    return new TypeResolversInformationModelProperty(category,
                                                     parameterResolversNames,
                                                     outputResolverName,
                                                     attributesResolverName,
                                                     keysResolverName,
                                                     false,
                                                     false,
                                                     partialTypeKeyResolver);
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

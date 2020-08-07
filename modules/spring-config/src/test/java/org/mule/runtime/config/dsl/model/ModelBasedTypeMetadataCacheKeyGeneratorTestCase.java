/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.dsl.model;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Optional.empty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.forExtension;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newListValue;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newParameterGroup;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.MULE_NAME;
import static org.mule.runtime.internal.dsl.DslConstants.FLOW_ELEMENT_IDENTIFIER;

import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.app.declaration.api.fluent.ElementDeclarer;
import org.mule.runtime.app.declaration.api.fluent.ParameterListValue;
import org.mule.runtime.app.declaration.api.fluent.ParameterObjectValue;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.api.dsl.model.metadata.ModelBasedMetadataCacheIdGeneratorFactory;
import org.mule.runtime.config.api.dsl.processor.ArtifactConfig;
import org.mule.runtime.config.internal.model.ApplicationModel;
import org.mule.runtime.core.api.extension.MuleExtensionModelProvider;
import org.mule.runtime.core.internal.locator.ComponentLocator;
import org.mule.runtime.core.internal.metadata.cache.MetadataCacheId;
import org.mule.runtime.core.internal.metadata.cache.MetadataCacheIdGenerator;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;

public class ModelBasedTypeMetadataCacheKeyGeneratorTestCase extends AbstractDslModelTestCase {

  private static final Logger LOGGER = LoggerFactory.getLogger(ModelBasedMetadataCacheKeyGeneratorTestCase.class);
  private static final String MY_FLOW = "myFlow";
  private static final String MY_CONFIG = "myConfig";
  private static final String OPERATION_LOCATION = MY_FLOW + "/processors/0";
  public static final String MY_GLOBAL_TEMPLATE = "myGlobalTemplate";

  private static final String MY_OTHER_FLOW = "myOtherFlow";
  private static final String ANOTHER_OPERATION_LOCATION = MY_OTHER_FLOW + "/processors/0";

  private Set<ExtensionModel> extensions;
  private DslResolvingContext dslResolvingContext;
  private ElementDeclarer declarer;

  @Before
  public void setUp() throws Exception {
    extensions = ImmutableSet.<ExtensionModel>builder()
        .add(MuleExtensionModelProvider.getExtensionModel())
        .add(mockExtension)
        .build();

    dslResolvingContext = DslResolvingContext.getDefault(extensions);
    declarer = ElementDeclarer.forExtension(EXTENSION_NAME);
  }

  @Test
  public void operationsWithSameOutputType() throws Exception {
    MetadataCacheId keyParts = getIdForComponentOutputMetadata(getBaseApp(), OPERATION_LOCATION);
    LOGGER.debug(keyParts.toString());


    MetadataCacheId otherKeyParts = getIdForComponentOutputMetadata(getBaseApp(), ANOTHER_OPERATION_LOCATION);
    LOGGER.debug(otherKeyParts.toString());

    assertThat(keyParts, is(otherKeyParts));
  }

  @Test
  public void operationsWithDifferentOutputTypeResolvers() throws Exception {
    mockTypeResolversInformationModelPropertyWithOutputType(operation, "category", "resolverName");
    MetadataCacheId keyParts = getIdForComponentOutputMetadata(getBaseApp(), OPERATION_LOCATION);
    LOGGER.debug(keyParts.toString());

    mockTypeResolversInformationModelPropertyWithOutputType(anotherOperation, "category", "anotherResolverName");
    MetadataCacheId otherKeyParts = getIdForComponentOutputMetadata(getBaseApp(), ANOTHER_OPERATION_LOCATION);
    LOGGER.debug(otherKeyParts.toString());

    assertThat(keyParts, not(otherKeyParts));
  }

  @Test
  public void operationsWithOutputStaticTypeAgainstDynamicType() throws Exception {
    mockTypeResolversInformationModelPropertyWithOutputType(operation, "category", "resolverName");
    MetadataCacheId keyParts = getIdForComponentOutputMetadata(getBaseApp(), OPERATION_LOCATION);
    LOGGER.debug(keyParts.toString());

    removeTypeResolversInformationModelPropertyfromMock(anotherOperation);
    MetadataCacheId otherKeyParts = getIdForComponentOutputMetadata(getBaseApp(), ANOTHER_OPERATION_LOCATION);
    LOGGER.debug(otherKeyParts.toString());

    assertThat(keyParts, not(otherKeyParts));
  }

  @Test
  public void operationsWithDifferentOutputResolversCategory() throws Exception {
    mockTypeResolversInformationModelPropertyWithOutputType(operation, "category", "resolverName");
    MetadataCacheId keyParts = getIdForComponentOutputMetadata(getBaseApp(), OPERATION_LOCATION);
    LOGGER.debug(keyParts.toString());

    mockTypeResolversInformationModelPropertyWithOutputType(anotherOperation, "anotherCategory", "resolverName");
    MetadataCacheId otherKeyParts = getIdForComponentOutputMetadata(getBaseApp(), ANOTHER_OPERATION_LOCATION);
    LOGGER.debug(otherKeyParts.toString());

    assertThat(keyParts, not(otherKeyParts));
  }

  @Test
  public void operationsWithSameAttributesType() throws Exception {
    MetadataCacheId keyParts = getIdForComponentAttributesMetadata(getBaseApp(), OPERATION_LOCATION);
    LOGGER.debug(keyParts.toString());


    MetadataCacheId otherKeyParts = getIdForComponentAttributesMetadata(getBaseApp(), ANOTHER_OPERATION_LOCATION);
    LOGGER.debug(otherKeyParts.toString());

    assertThat(keyParts, is(otherKeyParts));
  }

  @Test
  public void operationsWithDifferentAttributesTypeResolvers() throws Exception {
    mockTypeResolversInformationModelPropertyWithAttributeType(operation, "category", "resolverName");
    MetadataCacheId keyParts = getIdForComponentAttributesMetadata(getBaseApp(), OPERATION_LOCATION);
    LOGGER.debug(keyParts.toString());

    mockTypeResolversInformationModelPropertyWithAttributeType(anotherOperation, "category", "anotherResolverName");
    MetadataCacheId otherKeyParts = getIdForComponentAttributesMetadata(getBaseApp(), ANOTHER_OPERATION_LOCATION);
    LOGGER.debug(otherKeyParts.toString());

    assertThat(keyParts, not(otherKeyParts));
  }

  @Test
  public void operationsWithAttributeStaticTypeAgainstDynamicType() throws Exception {
    removeTypeResolversInformationModelPropertyfromMock(operation);
    MetadataCacheId keyParts = getIdForComponentAttributesMetadata(getBaseApp(), OPERATION_LOCATION);
    LOGGER.debug(keyParts.toString());

    mockTypeResolversInformationModelPropertyWithAttributeType(anotherOperation, "category", "anotherResolverName");
    MetadataCacheId otherKeyParts = getIdForComponentAttributesMetadata(getBaseApp(), ANOTHER_OPERATION_LOCATION);
    LOGGER.debug(otherKeyParts.toString());

    assertThat(keyParts, not(otherKeyParts));
  }

  @Test
  public void operationsWithDifferentAttributesResolversCategory() throws Exception {
    mockTypeResolversInformationModelPropertyWithAttributeType(operation, "category", "resolverName");
    MetadataCacheId keyParts = getIdForComponentAttributesMetadata(getBaseApp(), OPERATION_LOCATION);
    LOGGER.debug(keyParts.toString());

    mockTypeResolversInformationModelPropertyWithAttributeType(anotherOperation, "anotherCategory", "resolverName");
    MetadataCacheId otherKeyParts = getIdForComponentAttributesMetadata(getBaseApp(), ANOTHER_OPERATION_LOCATION);
    LOGGER.debug(otherKeyParts.toString());

    assertThat(keyParts, not(otherKeyParts));
  }

  @Test
  public void operationsWithSameInputType() throws Exception {
    Map<String, String> parameterResolversNames = new HashMap<>();
    parameterResolversNames.put(LIST_NAME, LIST_NAME);
    parameterResolversNames.put(CONTENT_NAME, CONTENT_NAME);
    mockTypeResolversInformationModelPropertyWithInputTypes(operation, "category", parameterResolversNames);
    MetadataCacheId keyParts = getIdForComponentInputMetadata(getBaseApp(), OPERATION_LOCATION, LIST_NAME);
    LOGGER.debug(keyParts.toString());

    Map<String, String> anotherParameterResolversNames = new HashMap<>();
    anotherParameterResolversNames.put(LIST_NAME, LIST_NAME);
    anotherParameterResolversNames.put(ANOTHER_CONTENT_NAME, CONTENT_NAME);
    mockTypeResolversInformationModelPropertyWithInputTypes(anotherOperation, "category", anotherParameterResolversNames);
    MetadataCacheId otherKeyParts = getIdForComponentInputMetadata(getBaseApp(), ANOTHER_OPERATION_LOCATION, LIST_NAME);
    LOGGER.debug(otherKeyParts.toString());

    assertThat(keyParts, is(otherKeyParts));
  }

  @Test
  public void operationsWithDifferentInputTypeResolvers() throws Exception {
    Map<String, String> parameterResolversNames = new HashMap<>();
    parameterResolversNames.put(LIST_NAME, LIST_NAME);
    parameterResolversNames.put(CONTENT_NAME, CONTENT_NAME);

    mockTypeResolversInformationModelPropertyWithInputTypes(operation, "category", parameterResolversNames);
    MetadataCacheId keyParts = getIdForComponentInputMetadata(getBaseApp(), OPERATION_LOCATION, CONTENT_NAME);
    LOGGER.debug(keyParts.toString());

    Map<String, String> anotherParameterResolversNames = new HashMap<>();
    parameterResolversNames.put(LIST_NAME, LIST_NAME);
    parameterResolversNames.put(ANOTHER_CONTENT_NAME, "changed");
    mockTypeResolversInformationModelPropertyWithInputTypes(anotherOperation, "category", anotherParameterResolversNames);
    MetadataCacheId otherKeyParts =
        getIdForComponentInputMetadata(getBaseApp(), ANOTHER_OPERATION_LOCATION, ANOTHER_CONTENT_NAME);
    LOGGER.debug(otherKeyParts.toString());

    assertThat(keyParts, not(otherKeyParts));
  }

  @Test
  public void operationsWithInputTypeStaticTypeAgainstDynamicType() throws Exception {
    Map<String, String> parameterResolversNames = new HashMap<>();
    parameterResolversNames.put(LIST_NAME, LIST_NAME);
    parameterResolversNames.put(CONTENT_NAME, CONTENT_NAME);

    mockTypeResolversInformationModelPropertyWithInputTypes(operation, "category", parameterResolversNames);
    MetadataCacheId keyParts = getIdForComponentInputMetadata(getBaseApp(), OPERATION_LOCATION, CONTENT_NAME);
    LOGGER.debug(keyParts.toString());

    removeTypeResolversInformationModelPropertyfromMock(anotherOperation);
    MetadataCacheId otherKeyParts =
        getIdForComponentInputMetadata(getBaseApp(), ANOTHER_OPERATION_LOCATION, ANOTHER_CONTENT_NAME);
    LOGGER.debug(otherKeyParts.toString());

    assertThat(keyParts, not(otherKeyParts));
  }

  @Test
  public void operationsWithDifferentInputResolversCategory() throws Exception {
    Map<String, String> parameterResolversNames = new HashMap<>();
    parameterResolversNames.put(LIST_NAME, LIST_NAME);
    parameterResolversNames.put(CONTENT_NAME, CONTENT_NAME);

    mockTypeResolversInformationModelPropertyWithInputTypes(operation, "category", parameterResolversNames);
    MetadataCacheId keyParts = getIdForComponentInputMetadata(getBaseApp(), OPERATION_LOCATION, LIST_NAME);
    LOGGER.debug(keyParts.toString());

    mockTypeResolversInformationModelPropertyWithInputTypes(anotherOperation, "anotherCategory", parameterResolversNames);
    MetadataCacheId otherKeyParts = getIdForComponentInputMetadata(getBaseApp(), ANOTHER_OPERATION_LOCATION, LIST_NAME);
    LOGGER.debug(otherKeyParts.toString());

    assertThat(keyParts, not(otherKeyParts));
  }

  @Test
  public void operationsWithDifferentStaticOutputTypes() throws Exception {
    removeTypeResolversInformationModelPropertyfromMock(operation);
    MetadataCacheId keyParts = getIdForComponentOutputMetadata(getBaseApp(), OPERATION_LOCATION);
    LOGGER.debug(keyParts.toString());

    removeTypeResolversInformationModelPropertyfromMock(anotherOperation);
    MetadataCacheId otherKeyParts = getIdForComponentOutputMetadata(getBaseApp(), ANOTHER_OPERATION_LOCATION);
    LOGGER.debug(otherKeyParts.toString());

    assertThat(keyParts, not(otherKeyParts));
  }

  @Test
  public void operationsWithDifferentStaticAttributeTypes() throws Exception {
    removeTypeResolversInformationModelPropertyfromMock(operation);
    MetadataCacheId keyParts = getIdForComponentAttributesMetadata(getBaseApp(), OPERATION_LOCATION);
    LOGGER.debug(keyParts.toString());

    removeTypeResolversInformationModelPropertyfromMock(anotherOperation);
    MetadataCacheId otherKeyParts = getIdForComponentAttributesMetadata(getBaseApp(), ANOTHER_OPERATION_LOCATION);
    LOGGER.debug(otherKeyParts.toString());

    assertThat(keyParts, not(otherKeyParts));
  }

  @Test
  public void operationsWithDifferentStaticInputTypes() throws Exception {
    removeTypeResolversInformationModelPropertyfromMock(operation);
    MetadataCacheId keyParts = getIdForComponentInputMetadata(getBaseApp(), OPERATION_LOCATION, LIST_NAME);
    LOGGER.debug(keyParts.toString());

    removeTypeResolversInformationModelPropertyfromMock(anotherOperation);
    MetadataCacheId otherKeyParts = getIdForComponentInputMetadata(getBaseApp(), ANOTHER_OPERATION_LOCATION, LIST_NAME);
    LOGGER.debug(otherKeyParts.toString());

    assertThat(keyParts, not(otherKeyParts));
  }

  @Test
  public void operationWithStaticOutputAndInputTypes() throws Exception {
    removeTypeResolversInformationModelPropertyfromMock(operation);
    MetadataCacheId keyParts = getIdForComponentOutputMetadata(getBaseApp(), OPERATION_LOCATION);
    LOGGER.debug(keyParts.toString());

    MetadataCacheId otherKeyParts = getIdForComponentAttributesMetadata(getBaseApp(), ANOTHER_OPERATION_LOCATION);
    LOGGER.debug(otherKeyParts.toString());

    assertThat(keyParts, not(otherKeyParts));
  }

  @Test
  public void operationWithStaticInputTypes() throws Exception {
    removeTypeResolversInformationModelPropertyfromMock(operation);
    MetadataCacheId keyParts = getIdForComponentInputMetadata(getBaseApp(), OPERATION_LOCATION, LIST_NAME);
    LOGGER.debug(keyParts.toString());

    MetadataCacheId otherKeyParts = getIdForComponentInputMetadata(getBaseApp(), OPERATION_LOCATION, CONTENT_NAME);
    LOGGER.debug(otherKeyParts.toString());

    assertThat(keyParts, not(otherKeyParts));
  }

  @Test
  public void operationWithParametersOfSameInputType() throws Exception {
    Map<String, String> parameterResolversNames = new HashMap<>();
    parameterResolversNames.put(LIST_NAME, LIST_NAME);
    parameterResolversNames.put(CONTENT_NAME, LIST_NAME);
    mockTypeResolversInformationModelPropertyWithInputTypes(operation, "category", parameterResolversNames);
    MetadataCacheId keyParts = getIdForComponentInputMetadata(getBaseApp(), OPERATION_LOCATION, LIST_NAME);
    LOGGER.debug(keyParts.toString());

    MetadataCacheId otherKeyParts = getIdForComponentInputMetadata(getBaseApp(), OPERATION_LOCATION, CONTENT_NAME);
    LOGGER.debug(otherKeyParts.toString());

    assertThat(keyParts, is(otherKeyParts));
  }

  @Test
  public void operationWithParametersOfDifferentInputType() throws Exception {
    Map<String, String> parameterResolversNames = new HashMap<>();
    parameterResolversNames.put(LIST_NAME, LIST_NAME);
    parameterResolversNames.put(CONTENT_NAME, CONTENT_NAME);
    mockTypeResolversInformationModelPropertyWithInputTypes(operation, "category", parameterResolversNames);
    MetadataCacheId keyParts = getIdForComponentInputMetadata(getBaseApp(), OPERATION_LOCATION, LIST_NAME);
    LOGGER.debug(keyParts.toString());

    MetadataCacheId otherKeyParts = getIdForComponentInputMetadata(getBaseApp(), OPERATION_LOCATION, CONTENT_NAME);
    LOGGER.debug(otherKeyParts.toString());

    assertThat(keyParts, not(otherKeyParts));
  }

  @Test
  public void sameComponentsOnDifferentExtensionsGenerateDifferentHash() throws Exception {
    final String newExtensionModelName = "newMockedExtension";
    final String newNamespace = "new-mockns";
    final String newNamespaceUri = "http://www.mulesoft.org/schema/mule/new-mockns";
    final String newSchemaLocation = "http://www.mulesoft.org/schema/mule/new-mockns/current/mule-new-mockns.xsd";


    ExtensionModel newExtensionModel = mock(ExtensionModel.class);

    initializeExtensionMock(newExtensionModel);

    when(newExtensionModel.getName()).thenReturn(newExtensionModelName);
    when(newExtensionModel.getXmlDslModel()).thenReturn(XmlDslModel.builder()
        .setXsdFileName("new-mule-mockns.xsd")
        .setPrefix(newNamespace)
        .setNamespace(newNamespaceUri)
        .setSchemaLocation(newSchemaLocation)
        .setSchemaVersion("4.0")
        .build());
    when(newExtensionModel.getSubTypes()).thenReturn(emptySet());
    when(newExtensionModel.getImportedTypes()).thenReturn(emptySet());
    when(newExtensionModel.getXmlDslModel()).thenReturn(XmlDslModel.builder()
        .setXsdFileName(EMPTY)
        .setPrefix(newNamespace)
        .setNamespace(newNamespaceUri)
        .setSchemaLocation(newSchemaLocation)
        .setSchemaVersion(EMPTY)
        .build());

    extensions = ImmutableSet.<ExtensionModel>builder()
        .add(MuleExtensionModelProvider.getExtensionModel())
        .add(mockExtension)
        .add(newExtensionModel)
        .build();
    dslResolvingContext = DslResolvingContext.getDefault(extensions);

    ElementDeclarer newElementDeclarer = forExtension(newExtensionModelName);

    final String newFlowName = "myNewFlow";
    final String newConfigName = "myNewConfig";
    ArtifactDeclaration baseApp = getBaseApp();
    ArtifactDeclaration app = ElementDeclarer.newArtifact()
        .withGlobalElement(baseApp.getGlobalElements().get(0)) //config
        .withGlobalElement(baseApp.getGlobalElements().get(1)) //flow
        .withGlobalElement(
                           newElementDeclarer.newConfiguration(CONFIGURATION_NAME)
                               .withRefName(newConfigName)
                               .withParameterGroup(newParameterGroup()
                                   .withParameter(CONTENT_NAME, CONTENT_VALUE)
                                   .withParameter(BEHAVIOUR_NAME, BEHAVIOUR_VALUE)
                                   .withParameter(LIST_NAME, newListValue().withValue(ITEM_VALUE).build())
                                   .getDeclaration())
                               .withConnection(newElementDeclarer.newConnection(CONNECTION_PROVIDER_NAME)
                                   .withParameterGroup(newParameterGroup()
                                       .withParameter(CONTENT_NAME, CONTENT_VALUE)
                                       .withParameter(BEHAVIOUR_NAME, BEHAVIOUR_VALUE)
                                       .withParameter(LIST_NAME,
                                                      newListValue().withValue(ITEM_VALUE).build())
                                       .getDeclaration())
                                   .getDeclaration())
                               .getDeclaration())
        .withGlobalElement(
                           ElementDeclarer.forExtension(MULE_NAME)
                               .newConstruct(FLOW_ELEMENT_IDENTIFIER)
                               .withRefName(newFlowName)
                               .withComponent(
                                              newElementDeclarer.newOperation(OPERATION_NAME)
                                                  .withConfig(newConfigName)
                                                  .withParameterGroup(g -> g.withParameter(CONTENT_NAME, "nonKey"))
                                                  .getDeclaration())
                               .getDeclaration())
        .getDeclaration();

    final String extensionOperationLocation = OPERATION_LOCATION;
    final String newExtensionOperationLocation = newFlowName + "/processors/0";

    MetadataCacheId oldHash = getIdForComponentOutputMetadata(app, extensionOperationLocation);
    MetadataCacheId newHash = getIdForComponentOutputMetadata(app, newExtensionOperationLocation);

    assertThat(oldHash, is(not(newHash)));
  }

  @Test
  public void sameTypesOnDifferentOperationsWithDifferentKeyResolverGeneratesSameHash() throws Exception {
    final String category = "category";
    final String outputResolverName = "outputResolver";
    Map<String, String> parameterResolversNames = new HashMap<>();
    parameterResolversNames.put(LIST_NAME, LIST_NAME);
    final String attributesResolverName = "attributesResolver";

    mockTypeResolversInformationModelProperty(operation, category, outputResolverName, attributesResolverName,
                                              parameterResolversNames, "operationKeysResolver");
    mockTypeResolversInformationModelProperty(anotherOperation, category, outputResolverName, attributesResolverName,
                                              parameterResolversNames, "anotherOperationKeysResolver");

    ArtifactDeclaration baseApp = getBaseApp();
    MetadataCacheId operationOutputMetadataCacheId = getIdForComponentOutputMetadata(baseApp, OPERATION_LOCATION);
    LOGGER.debug(operationOutputMetadataCacheId.toString());

    MetadataCacheId operationListInputMetadataCacheId = getIdForComponentInputMetadata(baseApp, OPERATION_LOCATION, LIST_NAME);
    LOGGER.debug(operationListInputMetadataCacheId.toString());

    MetadataCacheId operationAttributesMetadataCacheId = getIdForComponentAttributesMetadata(baseApp, OPERATION_LOCATION);
    LOGGER.debug(operationAttributesMetadataCacheId.toString());

    MetadataCacheId anotherOperationOutputMetadataCacheId = getIdForComponentOutputMetadata(baseApp, ANOTHER_OPERATION_LOCATION);
    LOGGER.debug(anotherOperationOutputMetadataCacheId.toString());

    MetadataCacheId anotherOperationListInputMetadataCacheId =
        getIdForComponentInputMetadata(baseApp, ANOTHER_OPERATION_LOCATION, LIST_NAME);
    LOGGER.debug(anotherOperationListInputMetadataCacheId.toString());

    MetadataCacheId anotherOperationAttributesMetadataCacheId =
        getIdForComponentAttributesMetadata(baseApp, ANOTHER_OPERATION_LOCATION);
    LOGGER.debug(anotherOperationAttributesMetadataCacheId.toString());

    assertThat(operationOutputMetadataCacheId, is(anotherOperationOutputMetadataCacheId));
    assertThat(operationListInputMetadataCacheId, is(anotherOperationListInputMetadataCacheId));
    assertThat(operationAttributesMetadataCacheId, is(anotherOperationAttributesMetadataCacheId));
  }

  protected MetadataCacheId getIdForComponentOutputMetadata(ArtifactDeclaration declaration, String location) throws Exception {
    ApplicationModel app = loadApplicationModel(declaration);
    ComponentAst component = new Locator(app)
        .get(Location.builderFromStringRepresentation(location).build())
        .get();
    return createGenerator(app).getIdForComponentOutputMetadata(component).get();
  }

  protected MetadataCacheId getIdForComponentAttributesMetadata(ArtifactDeclaration declaration, String location)
      throws Exception {
    ApplicationModel app = loadApplicationModel(declaration);
    ComponentAst component = new Locator(app)
        .get(Location.builderFromStringRepresentation(location).build())
        .get();
    return createGenerator(app).getIdForComponentAttributesMetadata(component).get();
  }

  protected MetadataCacheId getIdForComponentInputMetadata(ArtifactDeclaration declaration, String location, String parameterName)
      throws Exception {
    ApplicationModel app = loadApplicationModel(declaration);
    ComponentAst component = new Locator(app)
        .get(Location.builderFromStringRepresentation(location).build())
        .get();
    return createGenerator(app).getIdForComponentInputMetadata(component, parameterName).get();
  }

  private ArtifactDeclaration getBaseApp() {
    return ElementDeclarer.newArtifact()
        .withGlobalElement(declarer.newConfiguration(CONFIGURATION_NAME)
            .withRefName(MY_CONFIG)
            .withParameterGroup(newParameterGroup()
                .withParameter(CONTENT_NAME, CONTENT_VALUE)
                .withParameter(BEHAVIOUR_NAME, BEHAVIOUR_VALUE)
                .withParameter(LIST_NAME, newListValue().withValue(ITEM_VALUE).build())
                .getDeclaration())
            .withConnection(declarer.newConnection(CONNECTION_PROVIDER_NAME)
                .withParameterGroup(newParameterGroup()
                    .withParameter(CONTENT_NAME, CONTENT_VALUE)
                    .withParameter(BEHAVIOUR_NAME, BEHAVIOUR_VALUE)
                    .withParameter(LIST_NAME,
                                   newListValue().withValue(ITEM_VALUE).build())
                    .getDeclaration())
                .getDeclaration())
            .getDeclaration())
        .withGlobalElement(ElementDeclarer.forExtension(MULE_NAME)
            .newConstruct(FLOW_ELEMENT_IDENTIFIER)
            .withRefName(MY_FLOW)
            .withComponent(
                           declarer.newOperation(OPERATION_NAME)
                               .withConfig(MY_CONFIG)
                               .withParameterGroup(g -> g
                                   .withParameter(CONTENT_NAME, "nonKey"))
                               .getDeclaration())
            .getDeclaration())
        .withGlobalElement(ElementDeclarer.forExtension(MULE_NAME)
            .newConstruct(FLOW_ELEMENT_IDENTIFIER)
            .withRefName(MY_OTHER_FLOW)
            .withComponent(
                           declarer.newOperation(ANOTHER_OPERATION_NAME)
                               .withConfig(MY_CONFIG)
                               .withParameterGroup(g -> g
                                   .withParameter(ANOTHER_CONTENT_NAME, "anotherNonKey"))
                               .getDeclaration())
            .getDeclaration())
        .withGlobalElement(declarer.newGlobalParameter("complexType")
            .withRefName(MY_GLOBAL_TEMPLATE)
            .withValue(ParameterObjectValue.builder()
                .withParameter("otherName", "simpleParam")
                .withParameter("myCamelCaseName", "someContent")
                .withParameter("numbers", ParameterListValue.builder()
                    .withValue("10")
                    .withValue("20")
                    .build())
                .build())
            .getDeclaration())
        .getDeclaration();
  }

  protected ApplicationModel loadApplicationModel(ArtifactDeclaration declaration) throws Exception {
    return new ApplicationModel(new ArtifactConfig.Builder().build(),
                                declaration, extensions, emptyMap(), empty(), empty(),
                                uri -> getClass().getResourceAsStream(uri));
  }

  private MetadataCacheIdGenerator<ComponentAst> createGenerator(ApplicationModel app) {
    return new ModelBasedMetadataCacheIdGeneratorFactory().create(dslResolvingContext, new Locator(app));
  }

  private static class Locator implements ComponentLocator<ComponentAst> {

    private final Map<Location, ComponentAst> components = new HashMap<>();

    Locator(ApplicationModel app) {
      app.topLevelComponentsStream().forEach(this::addComponent);
    }

    @Override
    public Optional<ComponentAst> get(Location location) {
      return Optional.ofNullable(components.get(location));
    }

    private Location getLocation(ComponentAst component) {
      return Location.builderFromStringRepresentation(component.getLocation().getLocation()).build();
    }

    private void addComponent(ComponentAst component) {
      components.put(getLocation(component), component);
      component.directChildrenStream().forEach(this::addComponent);
    }

  }

}

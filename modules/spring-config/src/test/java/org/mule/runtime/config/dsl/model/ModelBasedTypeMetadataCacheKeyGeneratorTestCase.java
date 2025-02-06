/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.dsl.model;

import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.forExtension;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newListValue;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newParameterGroup;
import static org.mule.runtime.config.internal.dsl.utils.DslConstants.FLOW_ELEMENT_IDENTIFIER;
import static org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider.MULE_NAME;
import static org.mule.test.allure.AllureConstants.SdkToolingSupport.SDK_TOOLING_SUPPORT;
import static org.mule.test.allure.AllureConstants.SdkToolingSupport.MetadataTypeResolutionStory.METADATA_CACHE;

import static java.util.Arrays.asList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.app.declaration.api.fluent.ElementDeclarer;
import org.mule.runtime.app.declaration.api.fluent.ParameterListValue;
import org.mule.runtime.app.declaration.api.fluent.ParameterObjectValue;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.serialization.ArtifactAstDeserializer;
import org.mule.runtime.ast.api.serialization.ArtifactAstSerializerProvider;
import org.mule.runtime.config.api.dsl.model.DslElementModelFactory;
import org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider;
import org.mule.runtime.metadata.api.cache.MetadataCacheId;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(SDK_TOOLING_SUPPORT)
@Story(METADATA_CACHE)
public class ModelBasedTypeMetadataCacheKeyGeneratorTestCase extends AbstractMetadataCacheIdGeneratorTestCase {

  private static final Logger LOGGER = LoggerFactory.getLogger(ModelBasedMetadataCacheKeyGeneratorTestCase.class);
  private static final String MY_FLOW = "myFlow";
  private static final String CONFIGLESS_FLOW = "configlessFlow";
  private static final String MY_CONFIG = "myConfig";
  private static final String OPERATION_LOCATION = MY_FLOW + "/processors/0";
  public static final String MY_GLOBAL_TEMPLATE = "myGlobalTemplate";

  private static final String MY_OTHER_FLOW = "myOtherFlow";
  private static final String ANOTHER_OPERATION_LOCATION = MY_OTHER_FLOW + "/processors/0";

  @Test
  public void operationsWithSameOutputType() throws Exception {
    final var app = loadApplicationModel("ModelBasedTypeMetadataCacheKeyGeneratorTestCase");
    MetadataCacheId keyParts = getIdForComponentOutputMetadata(app, getBaseApp(), OPERATION_LOCATION);
    LOGGER.debug(keyParts.toString());

    MetadataCacheId otherKeyParts = getIdForComponentOutputMetadata(app, getBaseApp(), ANOTHER_OPERATION_LOCATION);
    LOGGER.debug(otherKeyParts.toString());

    assertThat(keyParts, is(otherKeyParts));
  }

  @Test
  public void operationsWithDifferentOutputTypeResolvers() throws Exception {
    mockTypeResolversInformationModelPropertyWithOutputType(operation, "category", "resolverName");
    final var app = loadApplicationModel("ModelBasedTypeMetadataCacheKeyGeneratorTestCase");
    MetadataCacheId keyParts = getIdForComponentOutputMetadata(app, getBaseApp(), OPERATION_LOCATION);
    LOGGER.debug(keyParts.toString());

    mockTypeResolversInformationModelPropertyWithOutputType(anotherOperation, "category", "anotherResolverName");
    MetadataCacheId otherKeyParts = getIdForComponentOutputMetadata(app, getBaseApp(), ANOTHER_OPERATION_LOCATION);
    LOGGER.debug(otherKeyParts.toString());

    assertThat(keyParts, not(otherKeyParts));
  }

  @Test
  public void operationsWithOutputStaticTypeAgainstDynamicType() throws Exception {
    mockTypeResolversInformationModelPropertyWithOutputType(operation, "category", "resolverName");
    final var app = loadApplicationModel("ModelBasedTypeMetadataCacheKeyGeneratorTestCase");
    MetadataCacheId keyParts = getIdForComponentOutputMetadata(app, getBaseApp(), OPERATION_LOCATION);
    LOGGER.debug(keyParts.toString());

    removeTypeResolversInformationModelPropertyfromMock(anotherOperation);
    MetadataCacheId otherKeyParts = getIdForComponentOutputMetadata(app, getBaseApp(), ANOTHER_OPERATION_LOCATION);
    LOGGER.debug(otherKeyParts.toString());

    assertThat(keyParts, not(otherKeyParts));
  }

  @Test
  public void operationsWithDifferentOutputResolversCategory() throws Exception {
    mockTypeResolversInformationModelPropertyWithOutputType(operation, "category", "resolverName");
    final var app = loadApplicationModel("ModelBasedTypeMetadataCacheKeyGeneratorTestCase");
    MetadataCacheId keyParts = getIdForComponentOutputMetadata(app, getBaseApp(), OPERATION_LOCATION);
    LOGGER.debug(keyParts.toString());

    mockTypeResolversInformationModelPropertyWithOutputType(anotherOperation, "anotherCategory", "resolverName");
    MetadataCacheId otherKeyParts = getIdForComponentOutputMetadata(app, getBaseApp(), ANOTHER_OPERATION_LOCATION);
    LOGGER.debug(otherKeyParts.toString());

    assertThat(keyParts, not(otherKeyParts));
  }

  @Test
  public void operationsWithSameAttributesType() throws Exception {
    final var app = loadApplicationModel("ModelBasedTypeMetadataCacheKeyGeneratorTestCase");
    MetadataCacheId keyParts = getIdForComponentAttributesMetadata(app, getBaseApp(), OPERATION_LOCATION);
    LOGGER.debug(keyParts.toString());

    MetadataCacheId otherKeyParts = getIdForComponentAttributesMetadata(app, getBaseApp(), ANOTHER_OPERATION_LOCATION);
    LOGGER.debug(otherKeyParts.toString());

    assertThat(keyParts, is(otherKeyParts));
  }

  @Test
  public void operationsWithDifferentAttributesTypeResolvers() throws Exception {
    mockTypeResolversInformationModelPropertyWithAttributeType(operation, "category", "resolverName");
    final var app = loadApplicationModel("ModelBasedTypeMetadataCacheKeyGeneratorTestCase");
    MetadataCacheId keyParts = getIdForComponentAttributesMetadata(app, getBaseApp(), OPERATION_LOCATION);
    LOGGER.debug(keyParts.toString());

    mockTypeResolversInformationModelPropertyWithAttributeType(anotherOperation, "category", "anotherResolverName");
    MetadataCacheId otherKeyParts = getIdForComponentAttributesMetadata(app, getBaseApp(), ANOTHER_OPERATION_LOCATION);
    LOGGER.debug(otherKeyParts.toString());

    assertThat(keyParts, not(otherKeyParts));
  }

  @Test
  public void operationsWithAttributeStaticTypeAgainstDynamicType() throws Exception {
    removeTypeResolversInformationModelPropertyfromMock(operation);
    final var app = loadApplicationModel("ModelBasedTypeMetadataCacheKeyGeneratorTestCase");
    MetadataCacheId keyParts = getIdForComponentAttributesMetadata(app, getBaseApp(), OPERATION_LOCATION);
    LOGGER.debug(keyParts.toString());

    mockTypeResolversInformationModelPropertyWithAttributeType(anotherOperation, "category", "anotherResolverName");
    MetadataCacheId otherKeyParts = getIdForComponentAttributesMetadata(app, getBaseApp(), ANOTHER_OPERATION_LOCATION);
    LOGGER.debug(otherKeyParts.toString());

    assertThat(keyParts, not(otherKeyParts));
  }

  @Test
  public void operationsWithDifferentAttributesResolversCategory() throws Exception {
    mockTypeResolversInformationModelPropertyWithAttributeType(operation, "category", "resolverName");
    final var app = loadApplicationModel("ModelBasedTypeMetadataCacheKeyGeneratorTestCase");
    MetadataCacheId keyParts = getIdForComponentAttributesMetadata(app, getBaseApp(), OPERATION_LOCATION);
    LOGGER.debug(keyParts.toString());

    mockTypeResolversInformationModelPropertyWithAttributeType(anotherOperation, "anotherCategory", "resolverName");
    MetadataCacheId otherKeyParts = getIdForComponentAttributesMetadata(app, getBaseApp(), ANOTHER_OPERATION_LOCATION);
    LOGGER.debug(otherKeyParts.toString());

    assertThat(keyParts, not(otherKeyParts));
  }

  @Test
  public void operationsInputHashIdStructure() throws Exception {
    Map<String, String> parameterResolversNames = new HashMap<>();
    parameterResolversNames.put(CONTENT_NAME, CONTENT_NAME);
    mockTypeResolversInformationModelPropertyWithInputTypes(operation, "category", parameterResolversNames);
    final var app = loadApplicationModel("ModelBasedTypeMetadataCacheKeyGeneratorTestCase");
    MetadataCacheId keyParts = getIdForComponentInputMetadata(app, getBaseApp(), OPERATION_LOCATION, CONTENT_NAME);
    LOGGER.debug(keyParts.toString());

    assertThat(keyParts.getParts(), hasSize(6));
    assertThat(keyParts.getParts().get(2).getSourceElementName().get(), startsWith("category:"));
    assertThat(keyParts.getParts().get(3).getSourceElementName().get(), startsWith("resolver:"));
    assertThat(keyParts.getParts().get(4).getSourceElementName().get(), equalTo("Input"));
    assertThat(keyParts.getParts().get(5).getSourceElementName().get(), equalTo("metadataKeyValues"));
  }

  @Test
  public void operationsOutputHashIdStructure() throws Exception {
    mockTypeResolversInformationModelPropertyWithOutputType(operation, "category", "outputResolverName");
    final var app = loadApplicationModel("ModelBasedTypeMetadataCacheKeyGeneratorTestCase");
    MetadataCacheId keyParts = getIdForComponentOutputMetadata(app, getBaseApp(), OPERATION_LOCATION);
    LOGGER.debug(keyParts.toString());

    assertThat(keyParts.getParts(), hasSize(6));
    assertThat(keyParts.getParts().get(2).getSourceElementName().get(), startsWith("category:"));
    assertThat(keyParts.getParts().get(3).getSourceElementName().get(), startsWith("resolver:"));
    assertThat(keyParts.getParts().get(4).getSourceElementName().get(), equalTo("Output"));
    assertThat(keyParts.getParts().get(5).getSourceElementName().get(), equalTo("metadataKeyValues"));
  }

  @Test
  public void operationsOutputAttributesHashIdStructure() throws Exception {
    mockTypeResolversInformationModelPropertyWithAttributeType(operation, "category", "outputAttributesResolverName");
    final var app = loadApplicationModel("ModelBasedTypeMetadataCacheKeyGeneratorTestCase");
    MetadataCacheId keyParts = getIdForComponentAttributesMetadata(app, getBaseApp(), OPERATION_LOCATION);
    LOGGER.debug(keyParts.toString());

    assertThat(keyParts.getParts(), hasSize(6));
    assertThat(keyParts.getParts().get(2).getSourceElementName().get(), startsWith("category:"));
    assertThat(keyParts.getParts().get(3).getSourceElementName().get(), startsWith("resolver:"));
    assertThat(keyParts.getParts().get(4).getSourceElementName().get(), equalTo("Attributes"));
    assertThat(keyParts.getParts().get(5).getSourceElementName().get(), equalTo("metadataKeyValues"));
  }

  @Test
  public void operationsWithSameInputType() throws Exception {
    Map<String, String> parameterResolversNames = new HashMap<>();
    parameterResolversNames.put(LIST_NAME, LIST_NAME);
    parameterResolversNames.put(CONTENT_NAME, CONTENT_NAME);
    mockTypeResolversInformationModelPropertyWithInputTypes(operation, "category", parameterResolversNames);
    final var app = loadApplicationModel("ModelBasedTypeMetadataCacheKeyGeneratorTestCase");
    MetadataCacheId keyParts = getIdForComponentInputMetadata(app, getBaseApp(), OPERATION_LOCATION, LIST_NAME);
    LOGGER.debug(keyParts.toString());

    Map<String, String> anotherParameterResolversNames = new HashMap<>();
    anotherParameterResolversNames.put(LIST_NAME, LIST_NAME);
    anotherParameterResolversNames.put(ANOTHER_CONTENT_NAME, CONTENT_NAME);
    mockTypeResolversInformationModelPropertyWithInputTypes(anotherOperation, "category", anotherParameterResolversNames);
    MetadataCacheId otherKeyParts =
        getIdForComponentInputMetadata(loadApplicationModel("ModelBasedTypeMetadataCacheKeyGeneratorTestCase"),
                                       getBaseApp(), ANOTHER_OPERATION_LOCATION, LIST_NAME);
    LOGGER.debug(otherKeyParts.toString());

    assertThat(keyParts, is(otherKeyParts));
  }

  @Test
  public void operationsWithDifferentInputTypeResolvers() throws Exception {
    Map<String, String> parameterResolversNames = new HashMap<>();
    parameterResolversNames.put(LIST_NAME, LIST_NAME);
    parameterResolversNames.put(CONTENT_NAME, CONTENT_NAME);

    mockTypeResolversInformationModelPropertyWithInputTypes(operation, "category", parameterResolversNames);
    final var app = loadApplicationModel("ModelBasedTypeMetadataCacheKeyGeneratorTestCase");
    MetadataCacheId keyParts = getIdForComponentInputMetadata(app, getBaseApp(),
                                                              OPERATION_LOCATION, CONTENT_NAME);
    LOGGER.debug(keyParts.toString());

    Map<String, String> anotherParameterResolversNames = new HashMap<>();
    parameterResolversNames.put(LIST_NAME, LIST_NAME);
    parameterResolversNames.put(ANOTHER_CONTENT_NAME, "changed");
    mockTypeResolversInformationModelPropertyWithInputTypes(anotherOperation, "category", anotherParameterResolversNames);
    MetadataCacheId otherKeyParts =
        getIdForComponentInputMetadata(loadApplicationModel("ModelBasedTypeMetadataCacheKeyGeneratorTestCase"), getBaseApp(),
                                       ANOTHER_OPERATION_LOCATION, ANOTHER_CONTENT_NAME);
    LOGGER.debug(otherKeyParts.toString());

    assertThat(keyParts, not(otherKeyParts));
  }

  @Test
  public void operationsWithInputTypeStaticTypeAgainstDynamicType() throws Exception {
    Map<String, String> parameterResolversNames = new HashMap<>();
    parameterResolversNames.put(LIST_NAME, LIST_NAME);
    parameterResolversNames.put(CONTENT_NAME, CONTENT_NAME);

    mockTypeResolversInformationModelPropertyWithInputTypes(operation, "category", parameterResolversNames);
    final var app = loadApplicationModel("ModelBasedTypeMetadataCacheKeyGeneratorTestCase");
    MetadataCacheId keyParts = getIdForComponentInputMetadata(app, getBaseApp(),
                                                              OPERATION_LOCATION, CONTENT_NAME);
    LOGGER.debug(keyParts.toString());

    removeTypeResolversInformationModelPropertyfromMock(anotherOperation);
    MetadataCacheId otherKeyParts =
        getIdForComponentInputMetadata(app, getBaseApp(), ANOTHER_OPERATION_LOCATION, ANOTHER_CONTENT_NAME);
    LOGGER.debug(otherKeyParts.toString());

    assertThat(keyParts, not(otherKeyParts));
  }

  @Test
  public void operationsWithDifferentInputResolversCategory() throws Exception {
    Map<String, String> parameterResolversNames = new HashMap<>();
    parameterResolversNames.put(LIST_NAME, LIST_NAME);
    parameterResolversNames.put(CONTENT_NAME, CONTENT_NAME);

    mockTypeResolversInformationModelPropertyWithInputTypes(operation, "category", parameterResolversNames);
    final var app = loadApplicationModel("ModelBasedTypeMetadataCacheKeyGeneratorTestCase");
    MetadataCacheId keyParts = getIdForComponentInputMetadata(app, getBaseApp(), OPERATION_LOCATION, LIST_NAME);
    LOGGER.debug(keyParts.toString());

    mockTypeResolversInformationModelPropertyWithInputTypes(anotherOperation, "anotherCategory", parameterResolversNames);
    MetadataCacheId otherKeyParts = getIdForComponentInputMetadata(app, getBaseApp(), ANOTHER_OPERATION_LOCATION, LIST_NAME);
    LOGGER.debug(otherKeyParts.toString());

    assertThat(keyParts, not(otherKeyParts));
  }

  @Test
  public void operationsWithDifferentStaticOutputTypes() throws Exception {
    removeTypeResolversInformationModelPropertyfromMock(operation);
    final var app = loadApplicationModel("ModelBasedTypeMetadataCacheKeyGeneratorTestCase");
    MetadataCacheId keyParts = getIdForComponentOutputMetadata(app, getBaseApp(), OPERATION_LOCATION);
    LOGGER.debug(keyParts.toString());

    removeTypeResolversInformationModelPropertyfromMock(anotherOperation);
    MetadataCacheId otherKeyParts = getIdForComponentOutputMetadata(app, getBaseApp(), ANOTHER_OPERATION_LOCATION);
    LOGGER.debug(otherKeyParts.toString());

    assertThat(keyParts, not(otherKeyParts));
  }

  @Test
  public void operationsWithDifferentStaticAttributeTypes() throws Exception {
    removeTypeResolversInformationModelPropertyfromMock(operation);
    final var app = loadApplicationModel("ModelBasedTypeMetadataCacheKeyGeneratorTestCase");
    MetadataCacheId keyParts = getIdForComponentAttributesMetadata(app, getBaseApp(), OPERATION_LOCATION);
    LOGGER.debug(keyParts.toString());

    removeTypeResolversInformationModelPropertyfromMock(anotherOperation);
    MetadataCacheId otherKeyParts = getIdForComponentAttributesMetadata(app, getBaseApp(), ANOTHER_OPERATION_LOCATION);
    LOGGER.debug(otherKeyParts.toString());

    assertThat(keyParts, not(otherKeyParts));
  }

  @Test
  public void operationsWithDifferentStaticInputTypes() throws Exception {
    removeTypeResolversInformationModelPropertyfromMock(operation);
    final var app = loadApplicationModel("ModelBasedTypeMetadataCacheKeyGeneratorTestCase");
    MetadataCacheId keyParts = getIdForComponentInputMetadata(app, getBaseApp(), OPERATION_LOCATION, LIST_NAME);
    LOGGER.debug(keyParts.toString());

    removeTypeResolversInformationModelPropertyfromMock(anotherOperation);
    MetadataCacheId otherKeyParts = getIdForComponentInputMetadata(app, getBaseApp(), ANOTHER_OPERATION_LOCATION, LIST_NAME);
    LOGGER.debug(otherKeyParts.toString());

    assertThat(keyParts, not(otherKeyParts));
  }

  @Test
  public void operationWithStaticOutputAndInputTypes() throws Exception {
    removeTypeResolversInformationModelPropertyfromMock(operation);
    final var app = loadApplicationModel("ModelBasedTypeMetadataCacheKeyGeneratorTestCase");
    MetadataCacheId keyParts = getIdForComponentOutputMetadata(app, getBaseApp(), OPERATION_LOCATION);
    LOGGER.debug(keyParts.toString());

    MetadataCacheId otherKeyParts = getIdForComponentAttributesMetadata(app, getBaseApp(), ANOTHER_OPERATION_LOCATION);
    LOGGER.debug(otherKeyParts.toString());

    assertThat(keyParts, not(otherKeyParts));
  }

  @Test
  public void operationWithStaticInputTypes() throws Exception {
    removeTypeResolversInformationModelPropertyfromMock(operation);
    final var app = loadApplicationModel("ModelBasedTypeMetadataCacheKeyGeneratorTestCase");
    MetadataCacheId keyParts = getIdForComponentInputMetadata(app, getBaseApp(), OPERATION_LOCATION, LIST_NAME);
    LOGGER.debug(keyParts.toString());

    MetadataCacheId otherKeyParts = getIdForComponentInputMetadata(app, getBaseApp(), OPERATION_LOCATION, CONTENT_NAME);
    LOGGER.debug(otherKeyParts.toString());

    assertThat(keyParts, not(otherKeyParts));
  }

  @Test
  public void operationWithParametersOfSameInputType() throws Exception {
    Map<String, String> parameterResolversNames = new HashMap<>();
    parameterResolversNames.put(LIST_NAME, LIST_NAME);
    parameterResolversNames.put(CONTENT_NAME, LIST_NAME);
    mockTypeResolversInformationModelPropertyWithInputTypes(operation, "category", parameterResolversNames);
    final var app = loadApplicationModel("ModelBasedTypeMetadataCacheKeyGeneratorTestCase");
    MetadataCacheId keyParts = getIdForComponentInputMetadata(app, getBaseApp(), OPERATION_LOCATION, LIST_NAME);
    LOGGER.debug(keyParts.toString());

    MetadataCacheId otherKeyParts = getIdForComponentInputMetadata(app, getBaseApp(), OPERATION_LOCATION, CONTENT_NAME);
    LOGGER.debug(otherKeyParts.toString());

    assertThat(keyParts, is(otherKeyParts));
  }

  @Test
  public void operationWithParametersOfDifferentInputType() throws Exception {
    Map<String, String> parameterResolversNames = new HashMap<>();
    parameterResolversNames.put(LIST_NAME, LIST_NAME);
    parameterResolversNames.put(CONTENT_NAME, CONTENT_NAME);
    mockTypeResolversInformationModelPropertyWithInputTypes(operation, "category", parameterResolversNames);
    final var app = loadApplicationModel("ModelBasedTypeMetadataCacheKeyGeneratorTestCase");
    MetadataCacheId keyParts = getIdForComponentInputMetadata(app, getBaseApp(), OPERATION_LOCATION, LIST_NAME);
    LOGGER.debug(keyParts.toString());

    MetadataCacheId otherKeyParts = getIdForComponentInputMetadata(app, getBaseApp(), OPERATION_LOCATION, CONTENT_NAME);
    LOGGER.debug(otherKeyParts.toString());

    assertThat(keyParts, not(otherKeyParts));
  }

  @Test
  public void sameComponentsOnDifferentExtensionsGenerateDifferentHash() throws Exception {
    final String newExtensionModelName = "newMockedExtension";
    final String newNamespace = "new-mockns";
    final String newNamespaceUri = "http://www.mulesoft.org/schema/mule/new-mockns";
    final String newSchemaLocation = "http://www.mulesoft.org/schema/mule/new-mockns/current/mule-new-mockns.xsd";


    ExtensionModel newExtensionModel = createExtension(newExtensionModelName, XmlDslModel.builder()
        .setXsdFileName("new-mule-mockns.xsd")
        .setPrefix(newNamespace)
        .setNamespace(newNamespaceUri)
        .setSchemaLocation(newSchemaLocation)
        .setSchemaVersion("4.0")
        .build(), asList(configuration), asList(connectionProvider));

    extensions = ImmutableSet.<ExtensionModel>builder()
        .add(MuleExtensionModelProvider.getExtensionModel())
        .add(mockExtension)
        .add(newExtensionModel)
        .build();
    dslResolvingContext = DslResolvingContext.getDefault(extensions);
    dslFactory = DslElementModelFactory.getDefault(dslResolvingContext);

    ElementDeclarer newElementDeclarer = forExtension(newExtensionModelName);

    final String newFlowName = "myNewFlow";
    final String newConfigName = "myNewConfig";
    ArtifactDeclaration baseApp = getBaseApp();
    ArtifactDeclaration app = ElementDeclarer.newArtifact()
        .withGlobalElement(baseApp.getGlobalElements().get(0)) // config
        .withGlobalElement(baseApp.getGlobalElements().get(1)) // flow
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
        .withGlobalElement(ElementDeclarer.forExtension(MULE_NAME)
            .newConstruct(FLOW_ELEMENT_IDENTIFIER)
            .withRefName(CONFIGLESS_FLOW)
            .withComponent(
                           newElementDeclarer.newOperation(OPERATION_NAME)
                               .withParameterGroup(g -> g
                                   .withParameter(CONTENT_NAME, "nonKey"))
                               .getDeclaration())
            .getDeclaration())

        .getDeclaration();

    // With config reference (which already includes the namespace:name for the configuration)
    final String extensionOperationLocation = OPERATION_LOCATION;
    final String newExtensionOperationLocation = newFlowName + "/processors/0";

    final var ast = loadApplicationModel("ModelBasedTypeMetadataCacheKeyGeneratorTestCase");
    final var modifiedAst = loadApplicationModel("ModelBasedTypeMetadataCacheKeyGeneratorTestCase_2");

    MetadataCacheId oldHash = getIdForComponentOutputMetadata(ast, baseApp, extensionOperationLocation);
    MetadataCacheId newHash = getIdForComponentOutputMetadata(modifiedAst, app, newExtensionOperationLocation);

    assertThat(oldHash, is(not(newHash)));

    // Without config reference
    final String configlessProcessorLocation = CONFIGLESS_FLOW + "/processors/0";

    oldHash = getIdForComponentOutputMetadata(ast, baseApp, configlessProcessorLocation);
    newHash = getIdForComponentOutputMetadata(modifiedAst, app, configlessProcessorLocation);

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
    final var app = loadApplicationModel("ModelBasedTypeMetadataCacheKeyGeneratorTestCase");
    MetadataCacheId operationOutputMetadataCacheId = getIdForComponentOutputMetadata(app, baseApp, OPERATION_LOCATION);
    LOGGER.debug(operationOutputMetadataCacheId.toString());

    MetadataCacheId operationListInputMetadataCacheId =
        getIdForComponentInputMetadata(app, baseApp, OPERATION_LOCATION, LIST_NAME);
    LOGGER.debug(operationListInputMetadataCacheId.toString());

    MetadataCacheId operationAttributesMetadataCacheId =
        getIdForComponentAttributesMetadata(app, baseApp, OPERATION_LOCATION);
    LOGGER.debug(operationAttributesMetadataCacheId.toString());

    MetadataCacheId anotherOperationOutputMetadataCacheId =
        getIdForComponentOutputMetadata(app, baseApp, ANOTHER_OPERATION_LOCATION);
    LOGGER.debug(anotherOperationOutputMetadataCacheId.toString());

    MetadataCacheId anotherOperationListInputMetadataCacheId =
        getIdForComponentInputMetadata(app, baseApp, ANOTHER_OPERATION_LOCATION, LIST_NAME);
    LOGGER.debug(anotherOperationListInputMetadataCacheId.toString());

    MetadataCacheId anotherOperationAttributesMetadataCacheId =
        getIdForComponentAttributesMetadata(app, baseApp, ANOTHER_OPERATION_LOCATION);
    LOGGER.debug(anotherOperationAttributesMetadataCacheId.toString());

    assertThat(operationOutputMetadataCacheId, is(anotherOperationOutputMetadataCacheId));
    assertThat(operationListInputMetadataCacheId, is(anotherOperationListInputMetadataCacheId));
    assertThat(operationAttributesMetadataCacheId, is(anotherOperationAttributesMetadataCacheId));
  }

  protected ArtifactAst loadApplicationModel(String astName) throws Exception {
    ArtifactAstDeserializer defaultArtifactAstDeserializer = new ArtifactAstSerializerProvider().getDeserializer();
    ArtifactAst deserializedArtifactAst = defaultArtifactAstDeserializer
        .deserialize(this.getClass().getResourceAsStream("/asts/" + astName + ".ast"),
                     name -> extensions.stream()
                         .filter(x -> x.getName().equals(name))
                         .findFirst()
                         .orElse(null));
    return deserializedArtifactAst;
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
                                   .withParameter(CONTENT_NAME, "nonKey")
                                   .withParameter(KEY_NAME, "keyA"))
                               .getDeclaration())
            .getDeclaration())
        .withGlobalElement(ElementDeclarer.forExtension(MULE_NAME)
            .newConstruct(FLOW_ELEMENT_IDENTIFIER)
            .withRefName(CONFIGLESS_FLOW)
            .withComponent(
                           declarer.newOperation(OPERATION_NAME)
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
                                   .withParameter(ANOTHER_CONTENT_NAME, "anotherNonKey")
                                   .withParameter(KEY_NAME, "keyA"))
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
}

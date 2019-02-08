/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.loader;

import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.ANY;
import static org.mule.runtime.core.internal.processor.chain.ModuleOperationMessageProcessorChainBuilder.MODULE_CONNECTION_GLOBAL_ELEMENT_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_PARAMETER_DESCRIPTION;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_VALUE_PARAMETER_DESCRIPTION;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_VALUE_PARAMETER_NAME;
import static org.mule.runtime.extension.api.declaration.type.ReconnectionStrategyTypeBuilder.RECONNECTION_CONFIG;
import static org.mule.runtime.extension.api.loader.xml.XmlExtensionModelLoader.RESOURCE_XML;
import static org.mule.runtime.extension.internal.loader.XmlExtensionLoaderDelegate.CONFIG_NAME;
import static org.mule.runtime.module.extension.api.loader.AbstractJavaExtensionModelLoader.TYPE_PROPERTY_NAME;
import static org.mule.runtime.module.extension.api.loader.AbstractJavaExtensionModelLoader.VERSION;
import org.mule.metadata.api.annotation.TypeIdAnnotation;
import org.mule.metadata.api.model.MetadataFormat;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.StringType;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.error.ErrorModelBuilder;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.config.internal.dsl.model.extension.xml.property.GlobalElementComponentModelModelProperty;
import org.mule.runtime.config.internal.dsl.model.extension.xml.property.OperationComponentModelModelProperty;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.loader.xml.XmlExtensionModelLoader;
import org.mule.runtime.module.extension.api.loader.java.DefaultJavaExtensionModelLoader;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.test.marvel.MarvelExtension;
import org.mule.test.petstore.extension.PetStoreConnector;

import com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class XmlExtensionLoaderTestCase extends AbstractMuleTestCase {

  private final boolean validateXml;

  @Parameterized.Parameters(name = "Validate XML: {0}")
  public static List<Object[]> parameters() {
    return asList(new Object[][] {
        {false},
        {true}
    });
  }

  /**
   * @param validateXml whether the XML must be valid while loading the extension model or not. Useful to determine if the default
   *        values are properly feed when reading the document.
   */
  public XmlExtensionLoaderTestCase(boolean validateXml) {
    this.validateXml = validateXml;
  }

  @Test
  public void testModuleSimple() {
    String modulePath = "modules/module-simple.xml";
    ExtensionModel extensionModel = getExtensionModelFrom(modulePath);

    assertThat(extensionModel.getName(), is("module-simple"));
    assertThat(extensionModel.getConfigurationModels().size(), is(0));
    assertThat(extensionModel.getModelProperty(GlobalElementComponentModelModelProperty.class).isPresent(), is(false));
    assertThat(extensionModel.getOperationModels().size(), is(11));

    Optional<OperationModel> operationModelOptional = extensionModel.getOperationModel("set-payload-concat-params-values");
    assertThat(operationModelOptional.isPresent(), is(true));
    final OperationModel operationModel = operationModelOptional.get();
    assertThat(operationModel.getAllParameterModels().size(), is(4));
    assertThat(operationModel.getAllParameterModels().get(0).getName(), is("value1"));
    assertThat(operationModel.getAllParameterModels().get(1).getName(), is("value2"));
    assertThat(operationModel.getAllParameterModels().get(2).getName(), is(TARGET_PARAMETER_NAME));
    assertThat(operationModel.getAllParameterModels().get(3).getName(), is(TARGET_VALUE_PARAMETER_NAME));

    Optional<OperationComponentModelModelProperty> modelProperty =
        operationModel.getModelProperty(OperationComponentModelModelProperty.class);
    assertThat(modelProperty.isPresent(), is(true));
    assertThat(modelProperty.get().getBodyComponentModel().getInnerComponents().size(), is(1));

    assertThat(operationModel.getOutput().getType().getMetadataFormat(), is(MetadataFormat.JAVA));
    assertThat(operationModel.getOutput().getType(), instanceOf(StringType.class));
    assertThat(operationModel.getOutputAttributes().getType().getMetadataFormat(), is(MetadataFormat.JAVA));
    assertThat(operationModel.getOutputAttributes().getType(), instanceOf(StringType.class));
    assertThat(operationModel.getErrorModels().size(), is(0));
  }

  @Test
  public void testModuleProperties() {
    String modulePath = "modules/module-properties.xml";
    ExtensionModel extensionModel = getExtensionModelFrom(modulePath);

    assertThat(extensionModel.getName(), is("module-properties"));
    assertThat(extensionModel.getConfigurationModels().size(), is(1));
    ConfigurationModel configurationModel = extensionModel.getConfigurationModels().get(0);
    assertThat(configurationModel.getName(), is(CONFIG_NAME));
    assertThat(configurationModel.getAllParameterModels().size(), is(5));
    assertThat(configurationModel.getAllParameterModels().get(0).getName(), is("configParam"));
    assertThat(configurationModel.getAllParameterModels().get(1).getName(), is("defaultConfigParam"));

    Optional<GlobalElementComponentModelModelProperty> globalElementComponentModelModelProperty =
        extensionModel.getModelProperty(GlobalElementComponentModelModelProperty.class);
    assertThat(globalElementComponentModelModelProperty.isPresent(), is(true));
    assertThat(globalElementComponentModelModelProperty.get().getGlobalElements().size(), is(1));

    assertThat(configurationModel.getOperationModels().size(), is(10));

    Optional<OperationModel> operationModel = configurationModel.getOperationModel("set-payload-add-param-and-property-values");
    assertThat(operationModel.isPresent(), is(true));
    assertThat(operationModel.get().getAllParameterModels().size(), is(3));
    assertThat(operationModel.get().getAllParameterModels().get(0).getName(), is("value1"));
    assertThat(operationModel.get().getAllParameterModels().get(1).getName(), is(TARGET_PARAMETER_NAME));
    assertThat(operationModel.get().getAllParameterModels().get(2).getName(), is(TARGET_VALUE_PARAMETER_NAME));


    Optional<OperationComponentModelModelProperty> modelProperty =
        operationModel.get().getModelProperty(OperationComponentModelModelProperty.class);
    assertThat(modelProperty.isPresent(), is(true));
    assertThat(modelProperty.get().getBodyComponentModel().getInnerComponents().size(), is(1));
  }

  @Test
  public void testModuleJsonCustomTypes() throws IOException {
    String modulePath = "modules/module-json-custom-types.xml";
    ExtensionModel extensionModel = getExtensionModelFrom(modulePath);

    assertThat(extensionModel.getName(), is("module-json-custom-types"));
    assertThat(extensionModel.getConfigurationModels().size(), is(0));
    assertThat(extensionModel.getModelProperty(GlobalElementComponentModelModelProperty.class).isPresent(), is(false));
    assertThat(extensionModel.getOperationModels().size(), is(7));

    Optional<OperationModel> operationModel = extensionModel.getOperationModel("operation-with-custom-types");
    assertThat(operationModel.isPresent(), is(true));
    final OperationModel operation = operationModel.get();
    assertThat(operation.getAllParameterModels().size(), is(3));
    final ParameterModel parameterValueModel = operation.getAllParameterModels().get(0);
    assertThat(parameterValueModel.getName(), is("value"));
    assertThat(operation.getAllParameterModels().get(1).getName(), is(TARGET_PARAMETER_NAME));
    assertThat(operation.getAllParameterModels().get(2).getName(), is(TARGET_VALUE_PARAMETER_NAME));

    assertThat(parameterValueModel.getType().getMetadataFormat(), is(MetadataFormat.JSON));
    assertThat(parameterValueModel.getType(), instanceOf(ObjectType.class));
    assertThat(((ObjectType) parameterValueModel.getType()).getFields().size(), is(2));

    assertThat(operation.getOutput().getType().getMetadataFormat(), is(MetadataFormat.JSON));
    assertThat(operation.getOutput().getType(), instanceOf(ObjectType.class));
    assertThat(((ObjectType) operation.getOutput().getType()).getFields().size(), is(3));

    Optional<OperationComponentModelModelProperty> modelProperty =
        operation.getModelProperty(OperationComponentModelModelProperty.class);
    assertThat(modelProperty.isPresent(), is(true));
    assertThat(modelProperty.get().getBodyComponentModel().getInnerComponents().size(), is(1));
  }

  @Test
  public void testModuleXsdCustomTypes() throws IOException {
    String modulePath = "modules/module-xsd-custom-types.xml";
    ExtensionModel extensionModel = getExtensionModelFrom(modulePath);

    assertThat(extensionModel.getName(), is("module-xsd-custom-types"));
    assertThat(extensionModel.getConfigurationModels().size(), is(0));
    assertThat(extensionModel.getModelProperty(GlobalElementComponentModelModelProperty.class).isPresent(), is(false));
    assertThat(extensionModel.getOperationModels().size(), is(4));

    Optional<OperationModel> operationModel = extensionModel.getOperationModel("operation-with-custom-types");
    assertThat(operationModel.isPresent(), is(true));
    final OperationModel operation = operationModel.get();
    assertThat(operation.getAllParameterModels().size(), is(4));
    assertThat(operation.getAllParameterModels().get(2).getName(), is(TARGET_PARAMETER_NAME));
    assertThat(operation.getAllParameterModels().get(3).getName(), is(TARGET_VALUE_PARAMETER_NAME));

    final ParameterModel firstParameterValueModel = operation.getAllParameterModels().get(0);
    assertThat(firstParameterValueModel.getName(), is("value"));
    assertThat(firstParameterValueModel.getType().getMetadataFormat(), is(MetadataFormat.XML));
    assertThat(firstParameterValueModel.getType(), instanceOf(ObjectType.class));
    final ObjectType firstInputParameterObjectType = (ObjectType) firstParameterValueModel.getType();
    assertThat(firstInputParameterObjectType.getFields().size(), is(1));
    assertThat(firstInputParameterObjectType.getFieldByName("User").isPresent(), is(true));

    final ParameterModel secondParameterValueModel = operation.getAllParameterModels().get(1);
    assertThat(secondParameterValueModel.getName(), is("value2"));
    assertThat(secondParameterValueModel.getType().getMetadataFormat(), is(MetadataFormat.XML));
    assertThat(secondParameterValueModel.getType(), instanceOf(ObjectType.class));
    final ObjectType secondInputParameterObjectType = (ObjectType) secondParameterValueModel.getType();
    assertThat(secondInputParameterObjectType.getFields().size(), is(1));
    assertThat(secondInputParameterObjectType.getFieldByName("Root").isPresent(), is(true));

    assertThat(operation.getOutput().getType().getMetadataFormat(), is(MetadataFormat.XML));
    assertThat(operation.getOutput().getType(), instanceOf(ObjectType.class));
    final ObjectType outputObjectType = (ObjectType) operation.getOutput().getType();
    assertThat(outputObjectType.getFields().size(), is(1));
    assertThat(outputObjectType.getFieldByName("Root0").isPresent(), is(true));

    Optional<OperationComponentModelModelProperty> modelProperty =
        operation.getModelProperty(OperationComponentModelModelProperty.class);
    assertThat(modelProperty.isPresent(), is(true));
    assertThat(modelProperty.get().getBodyComponentModel().getInnerComponents().size(), is(1));
  }

  @Test
  public void testModuleDocumentation() throws IOException {
    String modulePath = "modules/module-documentation.xml";
    List<String> expectedResources = Arrays.asList("resource1.txt", "functions.dwl");
    final ExtensionModel extensionModel = getExtensionModelFrom(modulePath, expectedResources);

    assertThat(extensionModel.getName(), is("module-documentation"));
    assertThat(extensionModel.getDescription(), is("Documentation for the connector"));
    assertThat(extensionModel.getResources(), is(containsInAnyOrder(expectedResources.toArray())));

    assertThat(extensionModel.getConfigurationModels().size(), is(1));
    ConfigurationModel configurationModel = extensionModel.getConfigurationModels().get(0);
    assertThat(configurationModel.getName(), is(CONFIG_NAME));
    final List<ParameterModel> configurationParameterModels = configurationModel.getAllParameterModels();
    assertThat(configurationParameterModels.size(), is(5));
    assertThat(configurationParameterModels.get(0).getName(), is("aPropertyWithDoc"));
    assertThat(configurationParameterModels.get(0).getDescription(), is("Documentation for the property"));

    assertThat(configurationParameterModels.get(1).getName(), is("aHiddenPropertyWithDoc"));
    assertThat(configurationParameterModels.get(1).getDescription(), is("Documentation for the hidden property"));
    assertThat(configurationParameterModels.get(1).getLayoutModel().isPresent(), is(true));
    assertThat(configurationParameterModels.get(1).getLayoutModel().get().isPassword(), is(true));

    assertThat(configurationParameterModels.get(2).getName(), is("aPropertyDisplayModel"));
    assertThat(configurationParameterModels.get(2).getDescription(), is(""));
    assertThat(configurationParameterModels.get(2).getDisplayModel().isPresent(), is(true));
    assertThat(configurationParameterModels.get(2).getDisplayModel().get().getDisplayName(), is("A pretty name property"));
    assertThat(configurationParameterModels.get(2).getDisplayModel().get().getSummary(), is("a summary tooltip property"));
    assertThat(configurationParameterModels.get(2).getDisplayModel().get().getExample(), is("SOME_PROPERTY_SAMPLE_DATA"));

    assertThat(configurationParameterModels.get(3).getName(), is("aPropertyWithPlacement"));
    assertThat(configurationParameterModels.get(3).getDescription(), is(""));
    assertThat(configurationParameterModels.get(3).getLayoutModel().isPresent(), is(true));
    assertThat(configurationParameterModels.get(3).getLayoutModel().get().getTabName().get(), is("Not General Property"));
    assertThat(configurationParameterModels.get(3).getLayoutModel().get().getOrder().get(), is(42));

    Optional<GlobalElementComponentModelModelProperty> globalElementComponentModelModelProperty =
        extensionModel.getModelProperty(GlobalElementComponentModelModelProperty.class);
    assertThat(globalElementComponentModelModelProperty.isPresent(), is(false));

    assertThat(configurationModel.getOperationModels().size(), is(2));

    Optional<OperationModel> operationModelOptional = configurationModel.getOperationModel("operation-with-doc");
    assertOperationWithDocumentationElementsEverywhere(extensionModel, operationModelOptional);

    Optional<OperationModel> anotherOperationModelOptional = configurationModel.getOperationModel("anotherOperationWithDoc");
    assertOperationWithDocumentationElementsEverywhere(extensionModel, anotherOperationModelOptional);
  }

  private void assertOperationWithDocumentationElementsEverywhere(final ExtensionModel extensionModel,
                                                                  final Optional<OperationModel> operationModelOptional) {
    assertThat(operationModelOptional.isPresent(), is(true));
    final OperationModel operationModel = operationModelOptional.get();
    assertThat(operationModel.getDescription(), is("Documentation for the operation"));
    assertThat(operationModel.getDisplayModel().isPresent(), is(true));
    assertThat(operationModel.getDisplayModel().get().getDisplayName(), is("A pretty name for the operation"));
    assertThat(operationModel.getDisplayModel().get().getSummary(), is("a summary tooltip operation"));
    assertThat(operationModel.getDisplayModel().get().getExample(), is("SOME_OPERATION_SAMPLE_DATA"));

    final List<ParameterModel> allParameterModels = operationModel.getAllParameterModels();
    assertThat(allParameterModels.size(), is(6));

    assertThat(allParameterModels.get(0).getName(), is("paramWithDoc"));
    assertThat(allParameterModels.get(0).getDescription(), is("Documentation for the parameter"));
    assertThat(allParameterModels.get(0).getLayoutModel().get().getTabName().get(), is(Placement.DEFAULT_TAB));
    assertThat(allParameterModels.get(0).getLayoutModel().get().getOrder().isPresent(), is(false));

    assertThat(allParameterModels.get(1).getName(), is("hiddenParamWithDoc"));
    assertThat(allParameterModels.get(1).getDescription(),
               is("Documentation for the hidden parameter"));
    assertThat(allParameterModels.get(1).getLayoutModel().isPresent(), is(true));
    assertThat(allParameterModels.get(1).getLayoutModel().get().isPassword(), is(true));

    assertThat(allParameterModels.get(2).getName(), is("paramDisplayModel"));
    assertThat(allParameterModels.get(2).getDescription(), is(""));
    assertThat(allParameterModels.get(2).getDisplayModel().isPresent(), is(true));
    assertThat(allParameterModels.get(2).getDisplayModel().get().getDisplayName(), is("A pretty name parameter"));
    assertThat(allParameterModels.get(2).getDisplayModel().get().getSummary(), is("a summary tooltip parameter"));
    assertThat(allParameterModels.get(2).getDisplayModel().get().getExample(), is("SOME_PARAMETER_SAMPLE_DATA"));

    assertThat(allParameterModels.get(3).getName(), is("paramWithPlacement"));
    assertThat(allParameterModels.get(3).getDescription(), is(""));
    assertThat(allParameterModels.get(3).getLayoutModel().isPresent(), is(true));
    assertThat(allParameterModels.get(3).getLayoutModel().get().getTabName().get(), is("Not General Parameter"));
    assertThat(allParameterModels.get(3).getLayoutModel().get().getOrder().get(), is(17));

    assertThat(allParameterModels.get(4).getName(), is(TARGET_PARAMETER_NAME));
    assertThat(allParameterModels.get(4).getDescription(), is(TARGET_PARAMETER_DESCRIPTION));

    assertThat(allParameterModels.get(5).getName(), is(TARGET_VALUE_PARAMETER_NAME));
    assertThat(allParameterModels.get(5).getDescription(), is(TARGET_VALUE_PARAMETER_DESCRIPTION));

    assertThat(operationModel.getOutput().getDescription(), is("Documentation for the output"));
    assertThat(operationModel.getOutputAttributes().getDescription(), is("Documentation for the output attributes"));
    assertThat(operationModel.getErrorModels().size(), is(2));
    assertThat(operationModel.getErrorModels(),
               containsInAnyOrder(
                                  ErrorModelBuilder.newError("CUSTOM_ERROR_HERE", extensionModel
                                      .getXmlDslModel().getPrefix().toUpperCase())
                                      .withParent(ErrorModelBuilder
                                          .newError(ANY).build())
                                      .build(),
                                  ErrorModelBuilder
                                      .newError("ANOTHER_CUSTOM_ERROR_HERE", extensionModel
                                          .getXmlDslModel().getPrefix().toUpperCase())
                                      .withParent(ErrorModelBuilder
                                          .newError(ANY).build())
                                      .build()));

    Optional<OperationComponentModelModelProperty> modelProperty =
        operationModel.getModelProperty(OperationComponentModelModelProperty.class);
    assertThat(modelProperty.isPresent(), is(true));
    assertThat(modelProperty.get().getBodyComponentModel().getInnerComponents().size(), is(1));
  }

  @Test
  public void testModuleCallingOperationsWithinModule() {
    String modulePath = "modules/module-calling-operations-within-module.xml";
    ExtensionModel extensionModel = getExtensionModelFrom(modulePath);

    assertThat(extensionModel.getName(), is("module-calling-operations-within-module"));
    assertThat(extensionModel.getConfigurationModels().size(), is(0));
    assertThat(extensionModel.getModelProperty(GlobalElementComponentModelModelProperty.class).isPresent(), is(false));
    assertThat(extensionModel.getOperationModels().size(), is(18));
  }

  @Test
  public void testModuleResources() {
    String modulePath = "modules/module-resources.xml";
    ExtensionModel extensionModel = getExtensionModelFrom(modulePath);

    assertThat(extensionModel.getName(), is("module-resources"));
    assertThat(extensionModel.getConfigurationModels().size(), is(0));
    assertThat(extensionModel.getModelProperty(GlobalElementComponentModelModelProperty.class).isPresent(), is(false));
    assertThat(extensionModel.getOperationModels().size(), is(0));
  }

  @Test
  public void testModuleTestConnection() {
    String modulePath = "modules/module-test-connection.xml";
    assertTestConnectionModuleOn(modulePath);
  }

  @Test
  public void testModuleTestConnectionMultipleConfigFirst() {
    String modulePath = "modules/module-test-connection-multiple-configs-first.xml";
    assertTestConnectionModuleOn(modulePath);
  }

  @Test
  public void testModuleTestConnectionMultipleConfigSecond() {
    String modulePath = "modules/module-test-connection-multiple-configs-second.xml";
    assertTestConnectionModuleOn(modulePath);
  }

  private void assertTestConnectionModuleOn(String modulePath) {
    ExtensionModel extensionModel = getExtensionModelFrom(modulePath);

    assertThat(extensionModel.getName(), is("module-test-connection"));
    assertThat(extensionModel.getConfigurationModels().size(), is(1));
    ConfigurationModel configurationModel = extensionModel.getConfigurationModels().get(0);
    assertThat(configurationModel.getName(), is(CONFIG_NAME));
    assertThat(configurationModel.getAllParameterModels().size(), is(3));

    Optional<ConnectionProviderModel> connectionProviderModel =
        configurationModel.getConnectionProviderModel(MODULE_CONNECTION_GLOBAL_ELEMENT_NAME);
    assertThat(connectionProviderModel.isPresent(), is(true));
    assertThat(connectionProviderModel.get().supportsConnectivityTesting(), is(true));
  }

  /**
   * If {@link #validateXml} is true, the XML of the smart connector must be validated when reading it. False otherwise. Useful to
   * simulate the {@link ExtensionModel} generation of a connector that has malformed message processors in the <body/> element.
   *
   * @param modulePath relative path to the XML connector.
   * @return an {@link ExtensionModel}
   */
  private ExtensionModel getExtensionModelFrom(String modulePath) {
    return getExtensionModelFrom(modulePath, emptyList());
  }

  /**
   * If {@link #validateXml} is true, the XML of the smart connector must be validated when reading it. False otherwise. Useful to
   * simulate the {@link ExtensionModel} generation of a connector that has malformed message processors in the <body/> element.
   *
   * @param modulePath relative path to the XML connector.
   * @param expectedResources list of resources to export
   * @return an {@link ExtensionModel}
   */
  private ExtensionModel getExtensionModelFrom(String modulePath, List<String> expectedResources) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(RESOURCE_XML, modulePath);
    parameters.put(XmlExtensionModelLoader.VALIDATE_XML, validateXml);
    if (!expectedResources.isEmpty()) {
      parameters.put(XmlExtensionModelLoader.RESOURCES_PATHS, expectedResources);
    }
    final ExtensionModel extensionModel = new XmlExtensionModelLoader().loadExtensionModel(getClass().getClassLoader(),
                                                                                           getDefault(getDependencyExtensions()),
                                                                                           parameters);
    assertNoReconnectionIsAdded(extensionModel);
    return extensionModel;
  }

  private void assertNoReconnectionIsAdded(ExtensionModel extensionModel) {
    new ExtensionWalker() {

      @Override
      protected void onParameter(ParameterizedModel owner, ParameterGroupModel groupModel, ParameterModel model) {

        model.getType().getAnnotation(TypeIdAnnotation.class).ifPresent(typeIdAnnotation -> {
          if (RECONNECTION_CONFIG.equals(typeIdAnnotation.getValue())) {
            fail("Reconnection element is not allowed in Smart Connectors");
          }
        });
      }
    }.walk(extensionModel);
  }

  private Set<ExtensionModel> getDependencyExtensions() {
    ExtensionModel petstore = loadExtension(PetStoreConnector.class, emptySet());
    ExtensionModel marvel = loadExtension(MarvelExtension.class, emptySet());
    return ImmutableSet.<ExtensionModel>builder().add(petstore).add(marvel).build();
  }

  private ExtensionModel loadExtension(Class extension, Set<ExtensionModel> deps) {
    DefaultJavaExtensionModelLoader loader = new DefaultJavaExtensionModelLoader();
    Map<String, Object> ctx = new HashMap<>();
    ctx.put(TYPE_PROPERTY_NAME, extension.getName());
    ctx.put(VERSION, "1.0.0-SNAPSHOT");
    return loader.loadExtensionModel(currentThread().getContextClassLoader(), DslResolvingContext.getDefault(deps), ctx);
  }
}

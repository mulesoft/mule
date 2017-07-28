/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.loader;

import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_OUTPUT_PARAMETER_DESCRIPTION;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_TYPE_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_PARAMETER_DESCRIPTION;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_PARAMETER_NAME;
import static org.mule.runtime.extension.internal.loader.XmlExtensionLoaderDelegate.CONFIG_NAME;
import static org.mule.runtime.extension.internal.loader.XmlExtensionModelLoader.RESOURCE_XML;
import org.junit.Test;
import org.mule.metadata.api.model.MetadataFormat;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.StringType;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.config.spring.internal.dsl.model.extension.xml.GlobalElementComponentModelModelProperty;
import org.mule.runtime.config.spring.internal.dsl.model.extension.xml.OperationComponentModelModelProperty;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class XmlExtensionLoaderTestCase extends AbstractMuleTestCase {

  private static final MuleVersion DEFAULT_MIN_MULE_VERSION = new MuleVersion("4.0.0");

  @Test
  public void testModuleSimple() {
    String modulePath = "modules/module-simple.xml";
    ExtensionModel extensionModel = getExtensionModelFrom(modulePath);

    assertThat(extensionModel.getName(), is("module-simple"));
    assertThat(extensionModel.getMinMuleVersion(), is(new MuleVersion("4.1.0")));
    assertThat(extensionModel.getConfigurationModels().size(), is(0));
    assertThat(extensionModel.getModelProperty(GlobalElementComponentModelModelProperty.class).isPresent(), is(false));
    assertThat(extensionModel.getOperationModels().size(), is(11));

    Optional<OperationModel> operationModelOptional = extensionModel.getOperationModel("set-payload-concat-params-values");
    assertThat(operationModelOptional.isPresent(), is(true));
    final OperationModel operationModel = operationModelOptional.get();
    assertThat(operationModel.getAllParameterModels().size(), is(3));
    assertThat(operationModel.getAllParameterModels().get(0).getName(), is("value1"));
    assertThat(operationModel.getAllParameterModels().get(1).getName(), is("value2"));
    assertThat(operationModel.getAllParameterModels().get(2).getName(), is(TARGET_PARAMETER_NAME));
    assertThat(operationModel.getAllParameterModels().get(3).getName(), is(TARGET_TYPE_PARAMETER_NAME));

    Optional<OperationComponentModelModelProperty> modelProperty =
        operationModel.getModelProperty(OperationComponentModelModelProperty.class);
    assertThat(modelProperty.isPresent(), is(true));
    assertThat(modelProperty.get().getBodyComponentModel().getInnerComponents().size(), is(1));

    assertThat(operationModel.getOutput().getType().getMetadataFormat(), is(MetadataFormat.JAVA));
    assertThat(operationModel.getOutput().getType(), instanceOf(StringType.class));
    assertThat(operationModel.getOutputAttributes().getType().getMetadataFormat(), is(MetadataFormat.JAVA));
    assertThat(operationModel.getOutputAttributes().getType(), instanceOf(StringType.class));
  }

  @Test
  public void testModuleProperties() {
    String modulePath = "modules/module-properties.xml";
    ExtensionModel extensionModel = getExtensionModelFrom(modulePath);

    assertThat(extensionModel.getName(), is("module-properties"));
    assertThat(extensionModel.getMinMuleVersion(), is(DEFAULT_MIN_MULE_VERSION));
    assertThat(extensionModel.getConfigurationModels().size(), is(1));
    ConfigurationModel configurationModel = extensionModel.getConfigurationModels().get(0);
    assertThat(configurationModel.getName(), is(CONFIG_NAME));
    assertThat(configurationModel.getAllParameterModels().size(), is(4));
    assertThat(configurationModel.getAllParameterModels().get(0).getName(), is("configParam"));
    assertThat(configurationModel.getAllParameterModels().get(1).getName(), is("defaultConfigParam"));

    Optional<GlobalElementComponentModelModelProperty> globalElementComponentModelModelProperty =
        configurationModel.getModelProperty(GlobalElementComponentModelModelProperty.class);
    assertThat(globalElementComponentModelModelProperty.isPresent(), is(true));
    assertThat(globalElementComponentModelModelProperty.get().getGlobalElements().size(), is(0));

    assertThat(configurationModel.getOperationModels().size(), is(7));

    Optional<OperationModel> operationModel = configurationModel.getOperationModel("set-payload-add-param-and-property-values");
    assertThat(operationModel.isPresent(), is(true));
    assertThat(operationModel.get().getAllParameterModels().size(), is(3));
    assertThat(operationModel.get().getAllParameterModels().get(0).getName(), is("value1"));
    assertThat(operationModel.get().getAllParameterModels().get(1).getName(), is(TARGET_PARAMETER_NAME));
    assertThat(operationModel.get().getAllParameterModels().get(2).getName(), is(TARGET_TYPE_PARAMETER_NAME));


    Optional<OperationComponentModelModelProperty> modelProperty =
        operationModel.get().getModelProperty(OperationComponentModelModelProperty.class);
    assertThat(modelProperty.isPresent(), is(true));
    assertThat(modelProperty.get().getBodyComponentModel().getInnerComponents().size(), is(1));
  }

  @Test
  public void testModuleGlobalElement() {
    String modulePath = "modules/module-global-element.xml";
    ExtensionModel extensionModel = getExtensionModelFrom(modulePath);

    assertThat(extensionModel.getName(), is("module-global-element"));
    assertThat(extensionModel.getMinMuleVersion(), is(DEFAULT_MIN_MULE_VERSION));
    assertThat(extensionModel.getConfigurationModels().size(), is(1));
    ConfigurationModel configurationModel = extensionModel.getConfigurationModels().get(0);
    assertThat(configurationModel.getName(), is(CONFIG_NAME));
    assertThat(configurationModel.getAllParameterModels().size(), is(4));
    assertThat(configurationModel.getAllParameterModels().get(0).getName(), is("someUserConfig"));
    assertThat(configurationModel.getAllParameterModels().get(1).getName(), is("somePassConfig"));
    assertThat(configurationModel.getAllParameterModels().get(2).getName(), is("port"));
    assertThat(configurationModel.getAllParameterModels().get(3).getName(), is("protocolConfig"));

    Optional<GlobalElementComponentModelModelProperty> globalElementComponentModelModelProperty =
        configurationModel.getModelProperty(GlobalElementComponentModelModelProperty.class);
    assertThat(globalElementComponentModelModelProperty.isPresent(), is(true));
    assertThat(globalElementComponentModelModelProperty.get().getGlobalElements().size(), is(1));

    assertThat(configurationModel.getOperationModels().size(), is(1));

    Optional<OperationModel> operationModel = configurationModel.getOperationModel("do-login");
    assertThat(operationModel.isPresent(), is(true));
    assertThat(operationModel.get().getAllParameterModels().size(), is(4));
    assertThat(operationModel.get().getAllParameterModels().get(0).getName(), is("someUser"));
    assertThat(operationModel.get().getAllParameterModels().get(1).getName(), is("somePass"));
    assertThat(operationModel.get().getAllParameterModels().get(2).getName(), is(TARGET_PARAMETER_NAME));
    assertThat(operationModel.get().getAllParameterModels().get(3).getName(), is(TARGET_TYPE_PARAMETER_NAME));


    Optional<OperationComponentModelModelProperty> modelProperty =
        operationModel.get().getModelProperty(OperationComponentModelModelProperty.class);
    assertThat(modelProperty.isPresent(), is(true));
    assertThat(modelProperty.get().getBodyComponentModel().getInnerComponents().size(), is(2));
  }

  @Test
  public void testModuleJsonCustomTypes() throws IOException {
    String modulePath = "modules/module-json-custom-types.xml";
    ExtensionModel extensionModel = getExtensionModelFrom(modulePath);

    assertThat(extensionModel.getName(), is("module-json-custom-types"));
    assertThat(extensionModel.getMinMuleVersion(), is(DEFAULT_MIN_MULE_VERSION));
    assertThat(extensionModel.getConfigurationModels().size(), is(0));
    assertThat(extensionModel.getModelProperty(GlobalElementComponentModelModelProperty.class).isPresent(), is(false));
    assertThat(extensionModel.getOperationModels().size(), is(3));

    Optional<OperationModel> operationModel = extensionModel.getOperationModel("operation-with-custom-types");
    assertThat(operationModel.isPresent(), is(true));
    final OperationModel operation = operationModel.get();
    assertThat(operation.getAllParameterModels().size(), is(3));
    final ParameterModel parameterValueModel = operation.getAllParameterModels().get(0);
    assertThat(parameterValueModel.getName(), is("value"));
    assertThat(operation.getAllParameterModels().get(1).getName(), is(TARGET_PARAMETER_NAME));
    assertThat(operation.getAllParameterModels().get(2).getName(), is(TARGET_TYPE_PARAMETER_NAME));

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
    assertThat(extensionModel.getMinMuleVersion(), is(DEFAULT_MIN_MULE_VERSION));
    assertThat(extensionModel.getConfigurationModels().size(), is(0));
    assertThat(extensionModel.getModelProperty(GlobalElementComponentModelModelProperty.class).isPresent(), is(false));
    assertThat(extensionModel.getOperationModels().size(), is(2));

    Optional<OperationModel> operationModel = extensionModel.getOperationModel("operation-with-custom-types");
    assertThat(operationModel.isPresent(), is(true));
    final OperationModel operation = operationModel.get();
    assertThat(operation.getAllParameterModels().size(), is(4));
    assertThat(operation.getAllParameterModels().get(2).getName(), is(TARGET_PARAMETER_NAME));
    assertThat(operation.getAllParameterModels().get(3).getName(), is(TARGET_TYPE_PARAMETER_NAME));

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
    ExtensionModel extensionModel = getExtensionModelFrom(modulePath);

    assertThat(extensionModel.getName(), is("module-documentation"));
    assertThat(extensionModel.getMinMuleVersion(), is(DEFAULT_MIN_MULE_VERSION));
    assertThat(extensionModel.getDescription(), is("Documentation for the connector"));
    assertThat(extensionModel.getConfigurationModels().size(), is(1));
    ConfigurationModel configurationModel = extensionModel.getConfigurationModels().get(0);
    assertThat(configurationModel.getName(), is(CONFIG_NAME));
    assertThat(configurationModel.getAllParameterModels().size(), is(2));
    assertThat(configurationModel.getAllParameterModels().get(0).getName(), is("aPropertyWithDoc"));
    assertThat(configurationModel.getAllParameterModels().get(0).getDescription(), is("Documentation for the property"));
    assertThat(configurationModel.getAllParameterModels().get(1).getName(), is("aHiddenPropertyWithDoc"));
    assertThat(configurationModel.getAllParameterModels().get(1).getDescription(), is("Documentation for the hidden property"));
    assertThat(configurationModel.getAllParameterModels().get(1).getLayoutModel().isPresent(), is(true));
    assertThat(configurationModel.getAllParameterModels().get(1).getLayoutModel().get().isPassword(), is(true));

    Optional<GlobalElementComponentModelModelProperty> globalElementComponentModelModelProperty =
        configurationModel.getModelProperty(GlobalElementComponentModelModelProperty.class);
    assertThat(globalElementComponentModelModelProperty.isPresent(), is(true));
    assertThat(globalElementComponentModelModelProperty.get().getGlobalElements().size(), is(0));

    assertThat(configurationModel.getOperationModels().size(), is(1));

    Optional<OperationModel> operationModel = configurationModel.getOperationModel("operation-with-doc");
    assertThat(operationModel.isPresent(), is(true));
    assertThat(operationModel.get().getDescription(), is("Documentation for the operation"));

    assertThat(operationModel.get().getAllParameterModels().size(), is(4));
    assertThat(operationModel.get().getAllParameterModels().get(0).getName(), is("paramWithDoc"));
    assertThat(operationModel.get().getAllParameterModels().get(0).getDescription(), is("Documentation for the parameter"));
    assertThat(operationModel.get().getAllParameterModels().get(1).getName(), is("hiddenParamWithDoc"));
    assertThat(operationModel.get().getAllParameterModels().get(1).getDescription(),
               is("Documentation for the hidden parameter"));
    assertThat(operationModel.get().getAllParameterModels().get(1).getLayoutModel().isPresent(), is(true));
    assertThat(operationModel.get().getAllParameterModels().get(1).getLayoutModel().get().isPassword(), is(true));
    assertThat(operationModel.get().getAllParameterModels().get(2).getName(), is(TARGET_PARAMETER_NAME));
    assertThat(operationModel.get().getAllParameterModels().get(2).getDescription(), is(TARGET_PARAMETER_DESCRIPTION));

    assertThat(operationModel.get().getAllParameterModels().get(3).getName(), is(TARGET_TYPE_PARAMETER_NAME));
    assertThat(operationModel.get().getAllParameterModels().get(3).getDescription(), is(TARGET_OUTPUT_PARAMETER_DESCRIPTION));

    assertThat(operationModel.get().getOutput().getDescription(), is("Documentation for the output"));
    assertThat(operationModel.get().getOutputAttributes().getDescription(), is("Documentation for the output attributes"));

    Optional<OperationComponentModelModelProperty> modelProperty =
        operationModel.get().getModelProperty(OperationComponentModelModelProperty.class);
    assertThat(modelProperty.isPresent(), is(true));
    assertThat(modelProperty.get().getBodyComponentModel().getInnerComponents().size(), is(1));
  }

  private ExtensionModel getExtensionModelFrom(String modulePath) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(RESOURCE_XML, modulePath);

    return new XmlExtensionModelLoader().loadExtensionModel(getClass().getClassLoader(), getDefault(emptySet()), parameters);
  }
}

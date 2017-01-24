/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.loader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_PARAMETER_NAME;
import static org.mule.runtime.extension.internal.loader.XmlExtensionLoaderDelegate.CONFIG_NAME;
import static org.mule.runtime.extension.internal.loader.XmlExtensionModelLoader.RESOURCE_XML;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Test;
import org.mule.metadata.api.model.MetadataFormat;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.json.handler.HandlerManager;
import org.mule.metadata.json.handler.ParsingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.config.spring.dsl.model.extension.xml.GlobalElementComponentModelModelProperty;
import org.mule.runtime.config.spring.dsl.model.extension.xml.OperationComponentModelModelProperty;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class XmlExtensionLoaderTestCase extends AbstractMuleTestCase {

  @Test
  public void testModuleSimple() {
    String modulePath = "module-simple/module-simple.xml";
    ExtensionModel extensionModel = getExtensionModelFrom(modulePath);

    assertThat(extensionModel.getName(), is("module-simple"));
    assertThat(extensionModel.getConfigurationModels().size(), is(0));
    assertThat(extensionModel.getModelProperty(GlobalElementComponentModelModelProperty.class).isPresent(), is(false));
    assertThat(extensionModel.getOperationModels().size(), is(8));

    Optional<OperationModel> operationModel = extensionModel.getOperationModel("set-payload-concat-params-values");
    assertThat(operationModel.isPresent(), is(true));
    assertThat(operationModel.get().getAllParameterModels().size(), is(3));
    assertThat(operationModel.get().getAllParameterModels().get(0).getName(), is("value1"));
    assertThat(operationModel.get().getAllParameterModels().get(1).getName(), is("value2"));
    assertThat(operationModel.get().getAllParameterModels().get(2).getName(), is(TARGET_PARAMETER_NAME));

    Optional<OperationComponentModelModelProperty> modelProperty =
        operationModel.get().getModelProperty(OperationComponentModelModelProperty.class);
    assertThat(modelProperty.isPresent(), is(true));
    assertThat(modelProperty.get().getComponentModel().getInnerComponents().size(), is(1));
  }

  @Test
  public void testModuleProperties() {
    String modulePath = "module-properties/module-properties.xml";
    ExtensionModel extensionModel = getExtensionModelFrom(modulePath);

    assertThat(extensionModel.getName(), is("module-properties"));
    assertThat(extensionModel.getConfigurationModels().size(), is(1));
    ConfigurationModel configurationModel = extensionModel.getConfigurationModels().get(0);
    assertThat(configurationModel.getName(), is(CONFIG_NAME));
    assertThat(configurationModel.getAllParameterModels().size(), is(2));
    assertThat(configurationModel.getAllParameterModels().get(0).getName(), is("configParam"));
    assertThat(configurationModel.getAllParameterModels().get(1).getName(), is("defaultConfigParam"));

    Optional<GlobalElementComponentModelModelProperty> globalElementComponentModelModelProperty =
        configurationModel.getModelProperty(GlobalElementComponentModelModelProperty.class);
    assertThat(globalElementComponentModelModelProperty.isPresent(), is(true));
    assertThat(globalElementComponentModelModelProperty.get().getGlobalElements().size(), is(0));

    assertThat(configurationModel.getOperationModels().size(), is(5));

    Optional<OperationModel> operationModel = configurationModel.getOperationModel("set-payload-add-param-and-property-values");
    assertThat(operationModel.isPresent(), is(true));
    assertThat(operationModel.get().getAllParameterModels().size(), is(2));
    assertThat(operationModel.get().getAllParameterModels().get(0).getName(), is("value1"));
    assertThat(operationModel.get().getAllParameterModels().get(1).getName(), is(TARGET_PARAMETER_NAME));


    Optional<OperationComponentModelModelProperty> modelProperty =
        operationModel.get().getModelProperty(OperationComponentModelModelProperty.class);
    assertThat(modelProperty.isPresent(), is(true));
    assertThat(modelProperty.get().getComponentModel().getInnerComponents().size(), is(1));
  }

  @Test
  public void testModuleGlobalElement() {
    String modulePath = "module-global-element/module-global-element.xml";
    ExtensionModel extensionModel = getExtensionModelFrom(modulePath);

    assertThat(extensionModel.getName(), is("module-global-element"));
    assertThat(extensionModel.getConfigurationModels().size(), is(1));
    ConfigurationModel configurationModel = extensionModel.getConfigurationModels().get(0);
    assertThat(configurationModel.getName(), is(CONFIG_NAME));
    assertThat(configurationModel.getAllParameterModels().size(), is(3));
    assertThat(configurationModel.getAllParameterModels().get(0).getName(), is("someUserConfig"));
    assertThat(configurationModel.getAllParameterModels().get(1).getName(), is("somePassConfig"));
    assertThat(configurationModel.getAllParameterModels().get(2).getName(), is("port"));

    Optional<GlobalElementComponentModelModelProperty> globalElementComponentModelModelProperty =
        configurationModel.getModelProperty(GlobalElementComponentModelModelProperty.class);
    assertThat(globalElementComponentModelModelProperty.isPresent(), is(true));
    assertThat(globalElementComponentModelModelProperty.get().getGlobalElements().size(), is(1));

    assertThat(configurationModel.getOperationModels().size(), is(1));

    Optional<OperationModel> operationModel = configurationModel.getOperationModel("do-login");
    assertThat(operationModel.isPresent(), is(true));
    assertThat(operationModel.get().getAllParameterModels().size(), is(3));
    assertThat(operationModel.get().getAllParameterModels().get(0).getName(), is("someUser"));
    assertThat(operationModel.get().getAllParameterModels().get(1).getName(), is("somePass"));
    assertThat(operationModel.get().getAllParameterModels().get(2).getName(), is(TARGET_PARAMETER_NAME));


    Optional<OperationComponentModelModelProperty> modelProperty =
        operationModel.get().getModelProperty(OperationComponentModelModelProperty.class);
    assertThat(modelProperty.isPresent(), is(true));
    assertThat(modelProperty.get().getComponentModel().getInnerComponents().size(), is(2));
  }

  @Test
  public void testModuleCustomTypes() throws IOException {
    String modulePath = "module-custom-types/module-custom-types.xml";
    ExtensionModel extensionModel = getExtensionModelFrom(modulePath);

    assertThat(extensionModel.getName(), is("module-custom-types"));
    assertThat(extensionModel.getConfigurationModels().size(), is(0));
    assertThat(extensionModel.getModelProperty(GlobalElementComponentModelModelProperty.class).isPresent(), is(false));
    assertThat(extensionModel.getOperationModels().size(), is(1));

    Optional<OperationModel> operationModel = extensionModel.getOperationModel("operation-with-custom-types");
    assertThat(operationModel.isPresent(), is(true));
    final OperationModel operation = operationModel.get();
    assertThat(operation.getAllParameterModels().size(), is(2));
    final ParameterModel parameterValueModel = operation.getAllParameterModels().get(0);
    assertThat(parameterValueModel.getName(), is("value"));
    assertThat(operation.getAllParameterModels().get(1).getName(), is(TARGET_PARAMETER_NAME));
    assertThat(parameterValueModel.getType(), is(loadTypeFrom("module-custom-types/type1-schema.json")));

    assertThat(operation.getOutput().getType().getMetadataFormat(), is(MetadataFormat.JSON));
    assertThat(operation.getOutput().getType(), is(loadTypeFrom("module-custom-types/type2-schema.json")));
    Optional<OperationComponentModelModelProperty> modelProperty =
        operation.getModelProperty(OperationComponentModelModelProperty.class);
    assertThat(modelProperty.isPresent(), is(true));
    assertThat(modelProperty.get().getComponentModel().getInnerComponents().size(), is(1));
  }

  private MetadataType loadTypeFrom(String file) throws IOException {
    try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(file)) {
      final JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
      final Schema jsonSchema = SchemaLoader.load(rawSchema);
      return new HandlerManager().handle(jsonSchema, new ParsingContext()).build();
    } catch (IOException e) {
      throw e;
    }
  }

  private ExtensionModel getExtensionModelFrom(String modulePath) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(RESOURCE_XML, modulePath);

    return new XmlExtensionModelLoader().loadExtensionModel(getClass().getClassLoader(), parameters);
  }
}

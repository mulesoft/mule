/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.introspection.describer;

import static java.lang.Thread.currentThread;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mule.runtime.extension.internal.introspection.describer.XmlBasedDescriber.CONFIG_NAME;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.config.spring.dsl.model.extension.GlobalElementComponentModelModelProperty;
import org.mule.runtime.config.spring.dsl.model.extension.OperationComponentModelModelProperty;
import org.mule.runtime.core.registry.SpiServiceRegistry;
import org.mule.runtime.extension.api.declaration.DescribingContext;
import org.mule.runtime.module.extension.internal.DefaultDescribingContext;
import org.mule.runtime.module.extension.internal.introspection.DefaultExtensionFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Optional;

import org.junit.Test;

public class XmlBasedDescriberTestCase extends AbstractMuleTestCase {

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
    assertThat(operationModel.get().getParameterModels().size(), is(2));
    assertThat(operationModel.get().getParameterModels().get(0).getName(), is("value1"));
    assertThat(operationModel.get().getParameterModels().get(1).getName(), is("value2"));

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
    assertThat(configurationModel.getParameterModels().size(), is(2));
    assertThat(configurationModel.getParameterModels().get(0).getName(), is("configParam"));
    assertThat(configurationModel.getParameterModels().get(1).getName(), is("defaultConfigParam"));

    Optional<GlobalElementComponentModelModelProperty> globalElementComponentModelModelProperty =
        configurationModel.getModelProperty(GlobalElementComponentModelModelProperty.class);
    assertThat(globalElementComponentModelModelProperty.isPresent(), is(true));
    assertThat(globalElementComponentModelModelProperty.get().getGlobalElements().size(), is(0));

    assertThat(extensionModel.getOperationModels().size(), is(5));

    Optional<OperationModel> operationModel = extensionModel.getOperationModel("set-payload-add-param-and-property-values");
    assertThat(operationModel.isPresent(), is(true));
    assertThat(operationModel.get().getParameterModels().size(), is(1));
    assertThat(operationModel.get().getParameterModels().get(0).getName(), is("value1"));


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
    assertThat(configurationModel.getParameterModels().size(), is(3));
    assertThat(configurationModel.getParameterModels().get(0).getName(), is("someUserConfig"));
    assertThat(configurationModel.getParameterModels().get(1).getName(), is("somePassConfig"));
    assertThat(configurationModel.getParameterModels().get(2).getName(), is("port"));

    Optional<GlobalElementComponentModelModelProperty> globalElementComponentModelModelProperty =
        configurationModel.getModelProperty(GlobalElementComponentModelModelProperty.class);
    assertThat(globalElementComponentModelModelProperty.isPresent(), is(true));
    assertThat(globalElementComponentModelModelProperty.get().getGlobalElements().size(), is(1));

    assertThat(extensionModel.getOperationModels().size(), is(1));

    Optional<OperationModel> operationModel = extensionModel.getOperationModel("do-login");
    assertThat(operationModel.isPresent(), is(true));
    assertThat(operationModel.get().getParameterModels().size(), is(2));
    assertThat(operationModel.get().getParameterModels().get(0).getName(), is("someUser"));
    assertThat(operationModel.get().getParameterModels().get(1).getName(), is("somePass"));


    Optional<OperationComponentModelModelProperty> modelProperty =
        operationModel.get().getModelProperty(OperationComponentModelModelProperty.class);
    assertThat(modelProperty.isPresent(), is(true));
    assertThat(modelProperty.get().getComponentModel().getInnerComponents().size(), is(2));
  }

  private ExtensionModel getExtensionModelFrom(String modulePath) {
    DescribingContext context = getContext();
    XmlBasedDescriber describer = new XmlBasedDescriber(modulePath);
    ExtensionDeclarer extensionDeclarer = describer.describe(context);

    DefaultExtensionFactory defaultExtensionFactory =
        new DefaultExtensionFactory(new SpiServiceRegistry(), currentThread().getContextClassLoader());
    return defaultExtensionFactory.createFrom(extensionDeclarer, context);
  }

  /**
   * @return the default implementation with the current test class loader
   */
  private DescribingContext getContext() {
    return new DefaultDescribingContext(getClass().getClassLoader());
  }

}

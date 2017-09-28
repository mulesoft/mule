/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.dsl.model.internal.config;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mule.test.allure.AllureConstants.ConfigurationProperties.CONFIGURATION_PROPERTIES;
import static org.mule.test.allure.AllureConstants.ConfigurationProperties.ComponentConfigurationAttributesStory.COMPONENT_CONFIGURATION_YAML_STORY;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.config.api.dsl.model.ResourceProvider;
import org.mule.runtime.config.internal.dsl.model.ClassLoaderResourceProvider;
import org.mule.runtime.config.internal.dsl.model.config.ConfigurationPropertiesComponent;
import org.mule.runtime.config.internal.dsl.model.config.ConfigurationPropertiesException;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Feature(CONFIGURATION_PROPERTIES)
@Story(COMPONENT_CONFIGURATION_YAML_STORY)
public class YamlConfigurationPropertiesComponentTestCase {

  @Rule
  public ExpectedException expectedException = none();

  private ResourceProvider externalResourceProvider;
  private ConfigurationPropertiesComponent configurationComponent;

  @Before
  public void setUp() {
    externalResourceProvider = new ClassLoaderResourceProvider(Thread.currentThread().getContextClassLoader());
  }

  @Description("Validates the values obtained for the different types in the properties")
  @Test
  public void validConfig() throws InitialisationException {
    configurationComponent = new ConfigurationPropertiesComponent("properties.yaml", externalResourceProvider);
    configurationComponent.initialise();
    assertThat(configurationComponent.getConfigurationProperty("number").get().getRawValue(), is("34843"));
    assertThat(configurationComponent.getConfigurationProperty("float").get().getRawValue(), is("2392.00"));
    assertThat(configurationComponent.getConfigurationProperty("date").get().getRawValue(), is("2001-01-23"));
    assertThat(configurationComponent.getConfigurationProperty("lines").get().getRawValue(), is("458 Walkman Dr.\nSuite #292\n"));
    assertThat(configurationComponent.getConfigurationProperty("list").get().getRawValue(), is("one,two,three"));
    assertThat(configurationComponent.getConfigurationProperty("complex.prop0").get().getRawValue(), is("value0"));
    assertThat(configurationComponent.getConfigurationProperty("complex.complex2.prop1").get().getRawValue(), is("value1"));
    assertThat(configurationComponent.getConfigurationProperty("complex.complex2.prop2").get().getRawValue(), is("value2"));
  }

  @Description("Validates that a list of complex objects is not valid")
  @Test
  public void complexListOfObjectsNotSuported() throws InitialisationException {
    configurationComponent = new ConfigurationPropertiesComponent("complex-list-object.yaml", externalResourceProvider);
    expectedException
        .expectMessage("Configuration properties does not support type a list of complex types. Complex type keys are: complex");
    expectedException.expect(ConfigurationPropertiesException.class);
    configurationComponent.initialise();
  }


  @Description("Validates that a only string types are supported")
  @Test
  public void unsupportedType() throws InitialisationException {
    configurationComponent = new ConfigurationPropertiesComponent("unsupported-type.yaml", externalResourceProvider);
    expectedException
        .expectMessage("YAML configuration properties only supports string values, make sure to wrap the value with \" so you force the value to be an string. Offending property is integer with value 1235");
    expectedException.expect(ConfigurationPropertiesException.class);
    configurationComponent.initialise();
  }

}

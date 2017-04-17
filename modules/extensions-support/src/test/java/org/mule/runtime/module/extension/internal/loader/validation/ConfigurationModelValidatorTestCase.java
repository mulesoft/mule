/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.test.module.extension.internal.util.ExtensionsTestUtils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@SmallTest
public class ConfigurationModelValidatorTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private ExtensionModelValidator validator = new ConfigurationModelValidator();

  @Test
  public void validConfigurationTypesForOperations() throws Exception {
    validate(ValidExtension.class);
  }

  @Test
  public void invalidConfigurationTypesForOperations() throws Exception {
    expectedException.expect(IllegalModelDefinitionException.class);
    expectedException.expectMessage("requires a configuration of type");
    validate(InvalidExtension.class);
  }

  private void validate(Class<?> connectorClass) {
    ExtensionsTestUtils.validate(connectorClass, validator);
  }

  interface ConfigInterface {

  }


  @Extension(name = "invalidExtension")
  @Configurations({InvalidTestConfig.class})
  public static class InvalidExtension {

  }


  @Extension(name = "validExtension")
  @Configurations({TestConfig.class, TestConfig2.class})
  public static class ValidExtension {

  }


  @Configuration(name = "config")
  @Operations(ValidTestOperations.class)
  public static class TestConfig implements ConfigInterface {

  }


  @Operations(InvalidTestOperations.class)
  public static class InvalidTestConfig implements ConfigInterface {

  }


  @Configuration(name = "config2")
  @Operations(ValidTestOperations.class)
  public static class TestConfig2 implements ConfigInterface {

  }


  public static class ValidTestOperations {

    public void foo(@Config ConfigInterface connection) {

    }

    public void bar(@Config ConfigInterface connection) {

    }
  }


  public static class InvalidTestOperations {

    public void foo(@Config ConfigInterface config) {

    }

    public void bar(@Config Apple config) {

    }
  }
}

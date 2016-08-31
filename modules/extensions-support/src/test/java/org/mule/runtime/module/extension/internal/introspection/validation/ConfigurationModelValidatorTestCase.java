/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.validation;

import static org.mule.runtime.core.config.MuleManifest.getProductVersion;
import org.mule.runtime.core.registry.SpiServiceRegistry;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.introspection.ExtensionFactory;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.declaration.DescribingContext;
import org.mule.runtime.module.extension.internal.DefaultDescribingContext;
import org.mule.runtime.module.extension.internal.exception.IllegalConfigurationModelDefinitionException;
import org.mule.runtime.module.extension.internal.introspection.DefaultExtensionFactory;
import org.mule.runtime.module.extension.internal.introspection.describer.AnnotationsBasedDescriber;
import org.mule.runtime.module.extension.internal.introspection.version.StaticVersionResolver;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.Apple;

import org.junit.Test;

@SmallTest
public class ConfigurationModelValidatorTestCase extends AbstractMuleTestCase {

  private ModelValidator validator = new ConfigurationModelValidator();
  private ExtensionFactory extensionFactory = new DefaultExtensionFactory(new SpiServiceRegistry(), getClass().getClassLoader());

  @Test
  public void validConfigurationTypesForOperations() throws Exception {
    validate(ValidExtension.class);
  }

  @Test(expected = IllegalConfigurationModelDefinitionException.class)
  public void invalidConfigurationTypesForOperations() throws Exception {
    validate(InvalidExtension.class);
  }

  private ExtensionModel modelFor(Class<?> connectorClass) {
    DescribingContext context = new DefaultDescribingContext(connectorClass.getClassLoader());
    return extensionFactory
        .createFrom(new AnnotationsBasedDescriber(connectorClass, new StaticVersionResolver(getProductVersion()))
            .describe(context), context);
  }

  private void validate(Class<?> connectorClass) {
    validator.validate(modelFor(connectorClass));
  }

  interface Config {

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
  public static class TestConfig implements Config {

  }


  @Operations(InvalidTestOperations.class)
  public static class InvalidTestConfig implements Config {

  }


  @Configuration(name = "config2")
  @Operations(ValidTestOperations.class)
  public static class TestConfig2 implements Config {

  }


  public static class ValidTestOperations {

    public void foo(@UseConfig Config connection) {

    }

    public void bar(@UseConfig Config connection) {

    }
  }


  public static class InvalidTestOperations {

    public void foo(@UseConfig Config config) {

    }

    public void bar(@UseConfig Apple config) {

    }
  }
}

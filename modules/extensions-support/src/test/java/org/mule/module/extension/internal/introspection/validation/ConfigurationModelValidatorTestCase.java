/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection.validation;

import org.mule.extension.annotation.api.Configuration;
import org.mule.extension.annotation.api.Configurations;
import org.mule.extension.annotation.api.Extension;
import org.mule.extension.annotation.api.Operation;
import org.mule.extension.annotation.api.Operations;
import org.mule.extension.annotation.api.param.UseConfig;
import org.mule.module.extension.internal.exception.IllegalConfigurationModelDefinitionException;
import org.mule.extension.api.introspection.ExtensionFactory;
import org.mule.extension.api.introspection.ExtensionModel;
import org.mule.module.extension.internal.DefaultDescribingContext;
import org.mule.module.extension.internal.introspection.describer.AnnotationsBasedDescriber;
import org.mule.module.extension.internal.introspection.DefaultExtensionFactory;
import org.mule.registry.SpiServiceRegistry;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class ConfigurationModelValidatorTestCase extends AbstractMuleTestCase
{

    private ModelValidator validator = new ConfigurationModelValidator();
    private ExtensionFactory extensionFactory = new DefaultExtensionFactory(new SpiServiceRegistry(), getClass().getClassLoader());

    @Test
    public void validConfigurationTypesForOperations() throws Exception
    {
        validate(ValidExtension.class);
    }

    @Test(expected = IllegalConfigurationModelDefinitionException.class)
    public void invalidConfigurationTypesForOperations() throws Exception
    {
        validate(InvalidExtension.class);
    }

    @Extension(name = "invalidExtension")
    @Configurations({TestConfig.class})
    @Operations(InvalidTestOperations.class)
    public static class InvalidExtension
    {

    }

    @Extension(name = "validExtension")
    @Configurations({TestConfig.class, TestConfig2.class})
    @Operations(ValidTestOperations.class)
    public static class ValidExtension
    {

    }

    @Configuration(name = "config")
    public static class TestConfig implements Config
    {

    }

    @Configuration(name = "config2")
    public static class TestConfig2 implements Config
    {

    }

    public static class ValidTestOperations
    {

        @Operation
        public void foo(@UseConfig Config connection)
        {

        }

        @Operation
        public void bar(@UseConfig Config connection)
        {

        }
    }

    public static class InvalidTestOperations
    {

        @Operation
        public void foo(@UseConfig Config connection)
        {

        }

        @Operation
        public void bar(@UseConfig TestConfig2 connection)
        {

        }
    }

    interface Config
    {

    }

    private ExtensionModel modelFor(Class<?> connectorClass)
    {
        return extensionFactory.createFrom(new AnnotationsBasedDescriber(connectorClass).describe(new DefaultDescribingContext()));
    }

    private void validate(Class<?> connectorClass)
    {
        validator.validate(modelFor(connectorClass));
    }
}

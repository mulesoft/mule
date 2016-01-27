/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection.validation;

import org.mule.api.connection.ConnectionException;
import org.mule.api.connection.ConnectionHandlingStrategy;
import org.mule.api.connection.ConnectionHandlingStrategyFactory;
import org.mule.api.connection.ConnectionValidationResult;
import org.mule.api.connection.ConnectionProvider;
import org.mule.extension.annotation.api.Alias;
import org.mule.extension.annotation.api.Configuration;
import org.mule.extension.annotation.api.Configurations;
import org.mule.extension.annotation.api.Extension;
import org.mule.extension.annotation.api.Operation;
import org.mule.extension.annotation.api.Operations;
import org.mule.extension.annotation.api.connector.Providers;
import org.mule.extension.annotation.api.param.Connection;
import org.mule.module.extension.internal.exception.IllegalConnectionProviderModelDefinitionException;
import org.mule.extension.api.exception.IllegalModelDefinitionException;
import org.mule.extension.api.introspection.ExtensionFactory;
import org.mule.extension.api.introspection.ExtensionModel;
import org.mule.module.extension.internal.DefaultDescribingContext;
import org.mule.module.extension.internal.introspection.describer.AnnotationsBasedDescriber;
import org.mule.module.extension.internal.introspection.DefaultExtensionFactory;
import org.mule.registry.SpiServiceRegistry;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.Apple;

import org.junit.Test;

@SmallTest
public class ConnectionProviderModelValidatorTestCase extends AbstractMuleTestCase
{

    private ModelValidator validator = new ConnectionProviderModelValidator();
    private ExtensionFactory extensionFactory = new DefaultExtensionFactory(new SpiServiceRegistry(), getClass().getClassLoader());

    @Test
    public void validModel() throws Exception
    {
        validate(ValidTestConnector.class);
    }

    @Test(expected = IllegalModelDefinitionException.class)
    public void invalidConnectedOperation()
    {
        validate(InvalidConnectedOperationTestConnector.class);
    }

    @Test(expected = IllegalConnectionProviderModelDefinitionException.class)
    public void invalidConfigConnectionProviderTestConnector()
    {
        validate(InvalidConfigConnectionProviderTestConnector.class);
    }

    @Test(expected = IllegalConnectionProviderModelDefinitionException.class)
    public void invalidConnectionTypeProviderTestConnector()
    {
        validate(InvalidConnectionTypeProviderTestConnector.class);
    }

    @Extension(name = "validatorTest")
    @Configurations({TestConfig.class, TestConfig2.class})
    @Operations(ValidTestOperations.class)
    @Providers({TestConnectionProvider.class, TestConnectionProvider2.class})
    public static class ValidTestConnector
    {

    }

    public static class ValidTestOperations
    {

        @Operation
        public void foo(@Connection ValidatorTestConnection connection)
        {

        }

        @Operation
        public void bar(@Connection ValidatorTestConnection connection)
        {

        }
    }

    @Extension(name = "validatorTest")
    @Configurations({TestConfig.class, TestConfig2.class})
    @Operations({ValidTestOperations.class, InvalidConnectionOperation.class})
    public static class InvalidConnectedOperationTestConnector
    {

    }

    public static class InvalidConnectionOperation
    {

        @Operation
        public void invalid(@Connection Apple apple)
        {
        }
    }

    @Extension(name = "validatorTest")
    @Configurations({TestConfig.class, TestConfig2.class})
    @Operations(ValidTestOperations.class)
    @Providers({TestConnectionProvider.class, TestConnectionProvider2.class, InvalidConfigConnectionProvider.class})
    public static class InvalidConfigConnectionProviderTestConnector
    {

    }

    @Extension(name = "validatorTest")
    @Configurations({TestConfig.class, TestConfig2.class})
    @Operations(ValidTestOperations.class)
    @Providers({TestConnectionProvider.class, TestConnectionProvider2.class, InvalidTypeConnectionProvider.class})
    public static class InvalidConnectionTypeProviderTestConnector
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

    @Alias("provider1")
    public static class TestConnectionProvider implements ConnectionProvider<Config, ValidatorTestConnection>
    {

        @Override
        public ValidatorTestConnection connect(Config config) throws ConnectionException
        {
            return new ValidatorTestConnection();
        }

        @Override
        public void disconnect(ValidatorTestConnection connection)
        {

        }

        @Override
        public ConnectionValidationResult validate(ValidatorTestConnection validatorTestConnection)
        {
            return ConnectionValidationResult.success();
        }

        @Override
        public ConnectionHandlingStrategy<ValidatorTestConnection> getHandlingStrategy(ConnectionHandlingStrategyFactory<Config, ValidatorTestConnection> handlingStrategyFactory)
        {
            return handlingStrategyFactory.cached();
        }
    }

    @Alias("provider2")
    public static class TestConnectionProvider2 extends TestConnectionProvider
    {

    }

    @Alias("invalidConfig")
    public static class InvalidConfigConnectionProvider implements ConnectionProvider<Apple, ValidatorTestConnection>
    {

        @Override
        public ValidatorTestConnection connect(Apple apple) throws ConnectionException
        {
            return new ValidatorTestConnection();
        }

        @Override
        public void disconnect(ValidatorTestConnection connection)
        {

        }

        @Override
        public ConnectionValidationResult validate(ValidatorTestConnection validatorTestConnection)
        {
            return ConnectionValidationResult.success();
        }

        @Override
        public ConnectionHandlingStrategy<ValidatorTestConnection> getHandlingStrategy(ConnectionHandlingStrategyFactory<Apple, ValidatorTestConnection> handlingStrategyFactory)
        {
            return handlingStrategyFactory.cached();
        }
    }

    @Alias("invalidConnection")
    public static class InvalidTypeConnectionProvider implements ConnectionProvider<Config, Apple>
    {

        @Override
        public Apple connect(Config config) throws ConnectionException
        {
            return new Apple();
        }

        @Override
        public void disconnect(Apple connection)
        {

        }

        @Override
        public ConnectionValidationResult validate(Apple apple)
        {
            return ConnectionValidationResult.success();
        }

        @Override
        public ConnectionHandlingStrategy<Apple> getHandlingStrategy(ConnectionHandlingStrategyFactory<Config, Apple> handlingStrategyFactory)
        {
            return handlingStrategyFactory.cached();
        }
    }

    interface Config
    {

    }

    public static class ValidatorTestConnection
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

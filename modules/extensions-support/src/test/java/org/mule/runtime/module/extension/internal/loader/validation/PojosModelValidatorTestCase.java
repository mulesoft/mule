/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static org.mule.runtime.api.connection.ConnectionValidationResult.success;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.module.extension.internal.loader.java.validation.PojosModelValidator;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.test.module.extension.internal.util.ExtensionsTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;


@SmallTest
public class PojosModelValidatorTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private ExtensionModelValidator validator = new PojosModelValidator();

  @Test
  public void pojoConfigurationMustOverrideEqualsAndHashCode() {
    expectedException.expect(IllegalModelDefinitionException.class);
    expectedException.expectMessage("Type 'InvalidPojoMustOverrideEqualsAndHashCode' must override equals and hashCode");
    validate(InvalidTestConnector.class);
  }

  @Test
  public void pojoConnectionProviderMustOverrideEqualsAndHashCode() {
    expectedException.expect(IllegalModelDefinitionException.class);
    expectedException.expectMessage("Type 'InvalidPojoMustOverrideEqualsAndHashCode' must override equals and hashCode");
    validate(InvalidConnectionProviderTestConnector.class);
  }

  @Test
  public void pojoImplementsEqualsAndHashCode() {
    validate(ValidTestConnector.class);
  }

  @Test
  public void pojoConfigurationMustHaveDefaultConstructor() {
    expectedException.expect(IllegalModelDefinitionException.class);
    expectedException.expectMessage("Type 'InvalidPojoRequiresDefaultConstructor' does not have a default constructor");
    validate(InvalidConfigParameterTestConnector.class);
  }


  private void validate(Class<?> connectorClass) {
    ExtensionsTestUtils.validate(connectorClass, validator, new HashMap() {

      {
        put("COMPILATION_MODE", true);
      }
    });
  }

  private static class InvalidPojoMustOverrideEqualsAndHashCode {

    private String id;
  }

  public static class PojoImplementsEqualsAndHashCode {

    private String id;

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;
      PojoImplementsEqualsAndHashCode that = (PojoImplementsEqualsAndHashCode) o;
      return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
      return Objects.hash(id);
    }
  }

  private static class Animal {

    private String name;

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;
      Animal animal = (Animal) o;
      return Objects.equals(name, animal.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name);
    }
  }

  public static class Dog extends Animal {

  }



  interface Config {

  }

  @Extension(name = "validatorTest")
  @Configurations({InvalidTestConfig.class})
  @ConnectionProviders({TestConnectionProvider.class})
  public static class InvalidTestConnector {

  }

  @Configuration(name = "config")
  @Operations(ValidTestOperations.class)
  public static class InvalidTestConfig implements Config {

    @Parameter
    private InvalidPojoMustOverrideEqualsAndHashCode pojo;
  }

  public static class ValidTestOperations {

    public void foo(@Connection ValidatorTestConnection connection) {

    }

    public void bar(@Connection ValidatorTestConnection connection) {

    }
  }

  @Alias("provider1")
  public static class TestConnectionProvider implements ConnectionProvider<ValidatorTestConnection> {

    @Parameter
    private Dog pojo;

    @Parameter
    private Map map;

    @Override
    public ValidatorTestConnection connect() throws ConnectionException {
      return new ValidatorTestConnection();
    }

    @Override
    public void disconnect(ValidatorTestConnection connection) {

    }

    @Override
    public ConnectionValidationResult validate(ValidatorTestConnection validatorTestConnection) {
      return success();
    }
  }

  @Alias("provider1")
  public static class InvalidTestConnectionProvider implements ConnectionProvider<ValidatorTestConnection> {

    @Parameter
    private InvalidPojoMustOverrideEqualsAndHashCode pojo;

    @Override
    public ValidatorTestConnection connect() throws ConnectionException {
      return new ValidatorTestConnection();
    }

    @Override
    public void disconnect(ValidatorTestConnection connection) {

    }

    @Override
    public ConnectionValidationResult validate(ValidatorTestConnection validatorTestConnection) {
      return success();
    }
  }

  public static class ValidatorTestConnection {

  }



  @Extension(name = "validatorTest")
  @Configurations({ValidTestConfig.class, ValidTestConfig2.class})
  @ConnectionProviders({TestConnectionProvider.class})
  public static class ValidTestConnector {

  }

  @Configuration(name = "config")
  @Operations(ValidTestOperations.class)
  public static class ValidTestConfig implements Config {

    @Parameter
    private PojoImplementsEqualsAndHashCode pojo;

    @Parameter
    private Map map;
  }

  @Configuration(name = "config2")
  @Operations(ValidTestOperations.class)
  public static class ValidTestConfig2 implements Config {

    @Parameter
    private Dog pojo;

  }

  @Extension(name = "validatorTest")
  @Configurations({ValidTestConfig.class})
  @ConnectionProviders({InvalidTestConnectionProvider.class})
  public static class InvalidConnectionProviderTestConnector {

  }


  public static class InvalidPojoRequiresDefaultConstructor {

    @Parameter
    private String bar;

    @Parameter
    private String id;

    public InvalidPojoRequiresDefaultConstructor(String bar, String id) {
      this.bar = bar;
      this.id = id;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;
      InvalidPojoRequiresDefaultConstructor that = (InvalidPojoRequiresDefaultConstructor) o;
      return Objects.equals(bar, that.bar) &&
          Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
      return Objects.hash(bar, id);
    }
  }

  @Configuration(name = "config3")
  @Operations(ValidTestOperations.class)
  public static class InvalidTestConfigPojoWithoutDefaultConstructor implements Config {

    @Parameter
    private InvalidPojoRequiresDefaultConstructor pojo;
  }

  @Extension(name = "validatorTest")
  @Configurations({InvalidTestConfigPojoWithoutDefaultConstructor.class})
  @ConnectionProviders({TestConnectionProvider.class})
  public static class InvalidConfigParameterTestConnector {

  }

}

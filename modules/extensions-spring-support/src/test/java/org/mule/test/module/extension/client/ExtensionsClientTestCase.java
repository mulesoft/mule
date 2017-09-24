/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.client;

import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mule.runtime.extension.api.client.DefaultOperationParameters.builder;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.HEISENBERG;
import static org.mule.test.heisenberg.extension.model.types.WeaponType.FIRE_WEAPON;
import static org.mule.test.vegan.extension.VeganExtension.VEGAN;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.extension.api.client.ExtensionsClient;
import org.mule.runtime.extension.api.client.OperationParameters;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.test.heisenberg.extension.HeisenbergOperations;
import org.mule.test.heisenberg.extension.model.KnockeableDoor;
import org.mule.test.heisenberg.extension.model.Ricin;
import org.mule.test.heisenberg.extension.model.Weapon;
import org.mule.test.heisenberg.extension.model.types.IntegerAttributes;
import org.mule.test.module.extension.AbstractHeisenbergConfigTestCase;
import org.mule.test.vegan.extension.VeganPolicy;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;

@Feature("EXTENSIONS_CLIENT")
public abstract class ExtensionsClientTestCase extends AbstractHeisenbergConfigTestCase {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  private static final String HEISENBERG_EXT_NAME = HEISENBERG;
  private static final String HEISENBERG_CONFIG = "heisenberg";
  private static final String ALIAS_OUTPUT = "jeje, my name is Juani and I'm 23 years old";

  @Inject
  protected ExtensionsClient client;

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    HeisenbergOperations.disposed = false;
  }

  @Override
  protected boolean isDisposeContextPerClass() {
    return true;
  }

  @After
  public void after() {
    HeisenbergOperations.disposed = false;
  }

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"vegan-config.xml", "heisenberg-config.xml"};
  }

  abstract <T, A> Result<T, A> doExecute(String extension, String operation, OperationParameters params)
      throws Throwable;

  @Test
  @Description("Executes a simple operation using the client and checks the output")
  public void executeSimpleOperation() throws Throwable {
    OperationParameters params = builder().configName(HEISENBERG_CONFIG)
        .addParameter("victim", "Juani")
        .addParameter("goodbyeMessage", "ADIOS")
        .build();
    Result<String, Object> result = doExecute(HEISENBERG_EXT_NAME, "kill", params);
    assertThat(result.getOutput(), is("ADIOS, Juani"));
  }

  @Test
  @Description("Executes a simple operation with an expression as parameter using the client and checks the output")
  public void executeSimpleOperationWithExpression() throws Throwable {
    OperationParameters params = builder().configName(HEISENBERG_CONFIG)
        .addParameter("victim", "#[mel:'Juani']")
        .addParameter("goodbyeMessage", "ADIOS")
        .build();
    Result<String, Object> result = doExecute(HEISENBERG_EXT_NAME, "kill", params);
    assertThat(result.getOutput(), is("ADIOS, Juani"));
  }

  @Test
  @Description("Executes an operation with a complex type parameter using the client and the DefaultOperationParametersBuilder")
  public void executeOperationWithComplexType() throws Throwable {
    Weapon.WeaponAttributes attributes = new Weapon.WeaponAttributes();
    attributes.setBrand("brand");
    OperationParameters params = builder()
        .configName(HEISENBERG_CONFIG)
        // Builds the complex type Ricin.
        .addParameter("weapon", Ricin.class, builder()
            .addParameter("destination", KnockeableDoor.class, builder()
                .addParameter("address", "ADdresss")
                .addParameter("victim", "victim!1231"))
            .addParameter("microgramsPerKilo", 123L))
        .addParameter("type", FIRE_WEAPON)
        .addParameter("attributesOfWeapon", attributes)
        .build();
    Result<String, Object> result = doExecute(HEISENBERG_EXT_NAME, "killWithWeapon", params);
    assertThat(result.getOutput(), is("Killed with: You have been killed with Ricin , Type FIRE_WEAPON and attribute brand"));
  }


  @Test
  @Description("Executes an operation that has a parameter group using the client and checks the output")
  public void executeOperationWithParameterGroup() throws Throwable {
    OperationParameters params = builder().configName(HEISENBERG_CONFIG)
        .addParameter("greeting", "jeje")
        .addParameter("age", 23)
        .addParameter("name", "Juani")
        .addParameter("knownAddresses", emptyList())
        .build();
    Result<String, Object> result = doExecute(HEISENBERG_EXT_NAME, "alias", params);
    assertThat(result.getOutput(), is(ALIAS_OUTPUT));
  }

  @Test
  @Description("Executes an operation that has a parameter with default value using the client and checks the output")
  public void executeOperationWithDefaultValueParameter() throws Throwable {
    OperationParameters params = builder().configName("apple").build();
    Result<Collection<?>, Object> result = doExecute(VEGAN, "tryToEatThisListOfMaps", params);
    assertThat(result.getOutput(), instanceOf(List.class));
    assertThat(result.getOutput(), hasSize(0));
  }

  @Test
  @Description("Executes an operation that has a @NullSafe annotated parameter using the client and checks the output")
  public void executeOperationWithNullSafeParameter() throws Throwable {
    OperationParameters params = builder().configName("banana").build();
    Result<VeganPolicy, Object> result = doExecute(VEGAN, "applyPolicy", params);
    assertThat(result.getOutput().getMeetAllowed(), is(false));
    assertThat(result.getOutput().getIngredients().getSaltMiligrams(), is(0));
  }

  @Test
  @Description("Executes an operation that returns attributes metadata using the client and checks the output and the attributes")
  public void executeOperationThatReturnsAttributes() throws Throwable {
    OperationParameters params = builder().configName(HEISENBERG_CONFIG).build();
    Result<String, IntegerAttributes> result = doExecute(HEISENBERG_EXT_NAME, "getEnemy", params);
    assertThat(result.getOutput(), is("Gustavo Fring"));
    assertThat(result.getAttributes().isPresent(), is(true));
    assertThat(result.getAttributes().get().getValue(), is(0));
  }

  @Test
  @Description("Executes a void operation using the client")
  public void executeVoidOperation() throws Throwable {
    OperationParameters params = builder().configName(HEISENBERG_CONFIG).build();
    Result<Object, Object> result = doExecute(HEISENBERG_EXT_NAME, "die", params);
    assertThat(result.getOutput(), is(nullValue()));
    assertThat(result.getAttributes().isPresent(), is(false));
  }

  @Test
  @Description("Executes an operation that fails using the client and checks the throwed exception")
  public void executeFailureOperation() throws Throwable {
    exception.expectCause(instanceOf(ConnectionException.class));
    exception.expectMessage("You are not allowed to speak with gus.");
    OperationParameters params = builder().configName(HEISENBERG_CONFIG).build();
    doExecute(HEISENBERG_EXT_NAME, "callGusFring", params);
  }

  @Test
  @Description("Tries to execute an operation from an extension that does not exist")
  public void nonExistentExtension() throws Throwable {
    exception.expect(MuleRuntimeException.class);
    exception.expectMessage("No Extension [no-exist] Found");
    OperationParameters params = builder().configName("config").build();
    doExecute("no-exist", "operation", params);
  }

  @Test
  @Description("Tries to execute an operation that does not exist")
  public void nonExistentOperation() throws Throwable {
    exception.expect(MuleRuntimeException.class);
    exception.expectMessage("No Operation [operationDontExist] Found");
    OperationParameters params = builder().configName("config").build();
    doExecute(VEGAN, "operationDontExist", params);
  }

  @Test
  @Description("Tries to execute an operation with a configuration that does not exist")
  public void nonExistentConfiguration() throws Throwable {
    exception.expect(MuleRuntimeException.class);
    exception.expectMessage("No configuration [configDontExist] found");
    OperationParameters params = builder().configName("configDontExist").build();
    doExecute(VEGAN, "applyPolicy", params);
  }

  @Test
  @Description("Checks that an operation disposes the resources after terminated")
  public void disposeAfterExecution() throws Throwable {
    executeSimpleOperation();
    assertThat(HeisenbergOperations.disposed, is(true));
  }

  @Test
  @Description("Checks that an operation disposes the resources when an error occurred while executing")
  public void disposeOnFailureOperation() throws Throwable {
    executeFailureOperation();
    assertThat(HeisenbergOperations.disposed, is(true));
  }
}

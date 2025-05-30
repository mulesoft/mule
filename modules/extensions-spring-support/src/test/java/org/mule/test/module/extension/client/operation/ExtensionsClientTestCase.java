/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.client.operation;

import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.runtime.core.api.util.IOUtils.closeQuietly;
import static org.mule.test.allure.AllureConstants.ExtensionsClientFeature.EXTENSIONS_CLIENT;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.HEISENBERG;
import static org.mule.test.vegan.extension.VeganExtension.VEGAN;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import static org.junit.Assert.assertThrows;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.extension.api.client.ExtensionsClient;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.test.heisenberg.extension.HeisenbergOperations;
import org.mule.test.heisenberg.extension.model.types.IntegerAttributes;
import org.mule.test.module.extension.AbstractHeisenbergConfigTestCase;
import org.mule.test.vegan.extension.VeganPolicy;

import java.io.Closeable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import jakarta.inject.Inject;

@Feature(EXTENSIONS_CLIENT)
public abstract class ExtensionsClientTestCase extends AbstractHeisenbergConfigTestCase {

  protected static final String HEISENBERG_EXT_NAME = HEISENBERG;
  protected static final String HEISENBERG_CONFIG = "heisenberg";
  private static final String ALIAS_OUTPUT = "jeje, my name is Juani and I'm 23 years old";
  private static final String ANOTHER_ALIAS_OUTPUT = "jeje, my name is Heisenberg and I'm 23 years old";

  @Inject
  protected ExtensionsClient client;

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    HeisenbergOperations.disposed = false;
  }

  @Override
  protected boolean isDisposeContextPerClass() {
    return false;
  }

  @After
  public void after() {
    HeisenbergOperations.disposed = false;
  }

  @Override
  protected String[] getConfigFiles() {
    return new String[] {
        "operations/vegan-config.xml",
        "operations/heisenberg-config.xml"};
  }

  protected abstract <T, A> Result<T, A> doExecute(String extension, String operation, Optional<String> configName,
                                                   Map<String, Object> params, boolean isPagedOperation,
                                                   boolean supportsStreaming)
      throws Throwable;

  private <T, A> Result<T, A> doExecute(String extension, String operation, String configName, Map<String, Object> params)
      throws Throwable {
    return doExecute(extension, operation, of(configName), params, false, false);
  }

  @Test
  @Description("Executes a simple operation using the client and checks the output")
  public void executeSimpleOperation() throws Throwable {
    Map<String, Object> params = new HashMap<>();
    params.put("victim", "Juani");
    params.put("goodbyeMessage", "ADIOS");
    Result<String, Object> result = doExecute(HEISENBERG_EXT_NAME, "kill", HEISENBERG_CONFIG, params);
    assertThat(result.getOutput(), is("ADIOS, Juani"));
  }

  @Test
  public void executePagedOperation() throws Throwable {
    Result<CursorIteratorProvider, Object> result =
        doExecute(HEISENBERG_EXT_NAME, "getPagedBlocklist", of(HEISENBERG_CONFIG), emptyMap(), true, false);
    CursorIteratorProvider provider = result.getOutput();
    AtomicInteger count = new AtomicInteger(0);
    Iterator<Message> iterator = provider.openCursor();
    try {
      iterator.forEachRemaining(m -> count.addAndGet(1));
      assertThat(count.get(), is(6));
    } finally {
      closeQuietly((Closeable) iterator);
      provider.close();
    }
  }

  @Test
  public void executeInputStreamOperation() throws Throwable {
    Result<CursorStreamProvider, Object> result =
        doExecute(HEISENBERG_EXT_NAME, "nameAsStream", of(HEISENBERG_CONFIG), emptyMap(), false, true);

    CursorStreamProvider streamProvider = result.getOutput();
    String value = IOUtils.toString(streamProvider);
    try {
      assertThat(value, equalTo("Heisenberg"));
    } finally {
      streamProvider.close();
    }
  }

  @Test
  @Description("Executes a simple operation twice using the client and checks the output")
  public void executeSimpleOperationTwice() throws Throwable {
    executeSimpleOperation();
    executeSimpleOperation();
  }

  @Test
  @Description("Executes a simple operation with an expression as parameter using the client and checks the output")
  public void executeSimpleOperationWithExpression() throws Throwable {
    Map<String, Object> params = new HashMap<>();
    params.put("victim", "#['Juani']");
    params.put("goodbyeMessage", "ADIOS");
    Result<String, Object> result = doExecute(HEISENBERG_EXT_NAME, "kill", HEISENBERG_CONFIG, params);
    assertThat(result.getOutput(), is("ADIOS, Juani"));
  }


  @Test
  @Description("Executes an operation that has a parameter group using the client and checks the output")
  public void executeOperationWithParameterGroup() throws Throwable {
    Map<String, Object> params = new HashMap<>();
    params.put("greeting", "jeje");
    params.put("age", 23);
    params.put("myName", "Juani");
    params.put("knownAddresses", emptyList());
    Result<String, Object> result = doExecute(HEISENBERG_EXT_NAME, "alias", HEISENBERG_CONFIG, params);
    assertThat(result.getOutput(), is(ALIAS_OUTPUT));
  }

  @Test
  @Description("Executes an operation that has a parameter group using the client and checks the output")
  public void executeOperationWithParameterGroupUsingOptional() throws Throwable {
    Map<String, Object> params = new HashMap<>();
    params.put("greeting", "jeje");
    params.put("age", 23);
    params.put("knownAddresses", emptyList());
    Result<String, Object> result = doExecute(HEISENBERG_EXT_NAME, "alias", HEISENBERG_CONFIG, params);
    assertThat(result.getOutput(), is(ANOTHER_ALIAS_OUTPUT));
  }

  @Test
  @Description("Executes a simple operation twice with different parameters using the client and checks the output")
  public void executeSimpleOperationTwiceWithDifferentParameters() throws Throwable {
    executeOperationWithParameterGroup();
    executeOperationWithParameterGroupUsingOptional();
  }

  @Test
  @Description("Executes a two different operationsusing the client and checks the output")
  public void executeTwoDifferentOperation() throws Throwable {
    executeOperationWithParameterGroup();
    executeSimpleOperation();
  }

  @Test
  @Description("Executes an operation that has a parameter with default value using the client and checks the output")
  public void executeOperationWithDefaultValueParameter() throws Throwable {
    Result<Collection<?>, Object> result = doExecute(VEGAN, "tryToEatThisListOfMaps", "apple", emptyMap());
    assertThat(result.getOutput(), instanceOf(List.class));
    assertThat(result.getOutput(), hasSize(0));
  }

  @Test
  @Description("Executes an operation that has a @NullSafe annotated parameter using the client and checks the output")
  public void executeOperationWithNullSafeParameter() throws Throwable {
    Result<VeganPolicy, Object> result = doExecute(VEGAN, "applyPolicy", "banana", emptyMap());
    assertThat(result.getOutput().getMeetAllowed(), is(false));
    assertThat(result.getOutput().getIngredients().getSaltMiligrams(), is(0));
  }

  @Test
  @Description("Executes an operation that returns attributes metadata using the client and checks the output and the attributes")
  public void executeOperationThatReturnsAttributes() throws Throwable {
    Result<String, IntegerAttributes> result = doExecute(HEISENBERG_EXT_NAME, "getEnemy", HEISENBERG_CONFIG, emptyMap());
    assertThat(result.getOutput(), is("Gustavo Fring"));
    assertThat(result.getAttributes().isPresent(), is(true));
    assertThat(result.getAttributes().get().getValue(), is(0));
  }

  @Test
  @Description("Executes a void operation using the client")
  public void executeVoidOperation() throws Throwable {
    Result<Object, Object> result = doExecute(HEISENBERG_EXT_NAME, "die", HEISENBERG_CONFIG, emptyMap());
    assertThat(result.getOutput(), is(nullValue()));
    assertThat(result.getAttributes().isPresent(), is(false));
  }

  @Test
  @Description("Executes an operation that fails using the client and checks the threw exception")
  public void executeFailureOperation() throws Throwable {
    var thrown =
        assertThrows(MuleException.class, () -> doExecute(HEISENBERG_EXT_NAME, "callGusFring", HEISENBERG_CONFIG, emptyMap()));
    assertThat(thrown.getCause(), instanceOf(ConnectionException.class));
    assertThat(thrown.getMessage(), containsString("You are not allowed to speak with gus."));
  }

  @Test
  @Description("Executes an operation that fails using the client and checks the threw exception")
  public void executeFailureNonBlockingOperation() throws Throwable {
    var thrown = assertThrows(MuleException.class,
                              () -> doExecute(HEISENBERG_EXT_NAME, "callGusFringNonBlocking", HEISENBERG_CONFIG, emptyMap()));
    assertThat(thrown.getCause(), instanceOf(ConnectionException.class));
    assertThat(thrown.getMessage(), containsString("You are not allowed to speak with gus."));
  }

  @Test
  @Description("Tries to execute an operation from an extension that does not exist")
  public void nonExistentExtension() throws Throwable {
    var thrown = assertThrows(MuleRuntimeException.class, () -> doExecute("no-exist", "operation", "config", emptyMap()));
    assertThat(thrown.getMessage(), containsString("No Extension [no-exist] Found"));
  }

  @Test
  @Description("Tries to execute an operation that does not exist")
  public void nonExistentOperation() throws Throwable {
    var thrown = assertThrows(MuleRuntimeException.class, () -> doExecute(VEGAN, "operationDontExist", "config", emptyMap()));
    assertThat(thrown.getMessage(), containsString("No Operation [operationDontExist] Found"));
  }

  @Test
  @Description("Tries to execute an operation with a configuration that does not exist")
  public void nonExistentConfiguration() throws Throwable {
    var thrown = assertThrows(MuleRuntimeException.class, () -> doExecute(VEGAN, "applyPolicy", "configDontExist", emptyMap()));
    assertThat(thrown.getMessage(), containsString("No configuration [configDontExist] found"));
  }

  @Test
  @Description("Tries to execute an operation that takes a long time")
  public void longOperation() throws Throwable {
    assertThat(doExecute(VEGAN, "longDigest", empty(), emptyMap(), false, false), not(nullValue()));
  }

  @Test
  @Issue("W-17524906")
  @Description("Tries to execute an operation with a TypedValue parameter")
  public void operationWithTypedValueParameter() throws Throwable {
    final String message = "Hello from ExtensionClient";

    Map<String, Object> params = new HashMap<>();
    params.put("message", new TypedValue<>(message, STRING));

    Result<Object, Object> result = doExecute(HEISENBERG_EXT_NAME, "echoStaticMessage", empty(), params, false, false);
    assertThat(result.getOutput(), is(message));
  }
}

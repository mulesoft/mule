/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.operation;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.test.allure.AllureConstants.ReuseFeature.REUSE;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.OPERATIONS;
import static org.mule.test.marvel.model.Villain.KABOOM;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.test.marvel.ironman.IronMan;

import java.util.List;
import java.util.Map;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(REUSE)
@Story(OPERATIONS)
public class MuleOperationExecutionTestCase extends MuleArtifactFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "mule-salutation-operations-config.xml";
  }

  @Test
  @Description("Calls a flow that executes the simple <this:hello-world> operation")
  public void executeHelloWorldOperation() throws Exception {
    assertHelloWorldResponse(flowRunner("salutationFlow").run());
  }

  @Test
  @Description("Calls a flow that executes the <this:hello-and-introduce> operation which is a composed one")
  public void executeComposedOperation() throws Exception {
    CoreEvent resultEvent = flowRunner("composedOperation").run();
    assertThat(resultEvent.getMessage().getPayload().getValue(),
               equalTo("good morning  Malaga!  Hello lad, my name is Anthony Stark"));
    assertThat(resultEvent.getMessage().getAttributes().getValue(), is(nullValue()));
  }

  @Test
  @Description("Calls a flow that executes the <this:hello-place> operation which takes exclusive optional parameters")
  public void executeExclusiveOptionalsOperation() throws Exception {
    CoreEvent resultEvent = flowRunner("exclusiveOptionalsFlow").run();
    assertThat(resultEvent.getMessage().getPayload().getValue(),
               equalTo("good morning  London "));
    assertThat(resultEvent.getMessage().getAttributes().getValue(), is(nullValue()));
  }

  @Test
  @Description("Calls a flow that executes the <this:non-blocking-hello-world> operation which is non blocking")
  public void executeNonBlockingOperation() throws Exception {
    CoreEvent resultEvent = flowRunner("nonBlockingOperationFlow").run();
    assertHelloWorldResponse(resultEvent);
  }

  @Test
  @Description("Calls a flow that executes the <this:blocking-hello-world> operation which is blocking")
  public void executeBlockingOperation() throws Exception {
    CoreEvent resultEvent = flowRunner("blockingOperationFlow").run();
    assertHelloWorldResponse(resultEvent);
  }

  @Test
  @Description("Calls a flow that executes the <this:repeated-hello-world> operation which is implemented recursively")
  public void executeRecursiveOperation() throws Exception {
    CoreEvent resultEvent = flowRunner("recursiveOperationFlow").run();
    assertThat(resultEvent.getMessage().getPayload().getValue(), equalTo("Hello,   Malaga! Hello,   Malaga! Hello,   Malaga! "));
    assertThat(resultEvent.getMessage().getAttributes().getValue(), is(nullValue()));
  }

  @Test
  @Description("Calls a flow that executes the <this:salute-aggressively> operation which is configurable")
  public void executeConfigurableOperation() throws Exception {
    ConfigurationInstance config = muleContext.getExtensionManager().getConfiguration("ironMan", testEvent());
    assertThat(config, is(notNullValue()));

    IronMan ironManConfig = (IronMan) config.getValue();
    assertThat(ironManConfig.getMissilesFired(), is(0));

    CoreEvent resultEvent = flowRunner("configurableOperationFlow").run();
    String result = (String) resultEvent.getMessage().getPayload().getValue();
    assertThat(result, is(KABOOM));

    assertThat(ironManConfig.getMissilesFired(), is(1));
  }

  @Test
  @Description("Verifies that operations params don't exit its own scope when composed")
  public void captureParamsAcrossOperations() throws Exception {
    CoreEvent resultEvent = flowRunner("interceptAndDumpParameters").run();
    assertThat(resultEvent.getParameters().size(), is(0));

    Map<String, List<Map<String, TypedValue<?>>>> dump =
        (Map<String, List<Map<String, TypedValue<?>>>>) resultEvent.getMessage().getPayload().getValue();
    assertThat(dump.size(), is(2));

    List<Map<String, TypedValue<?>>> interception = dump.get("helloWorld");
    assertThat(interception, hasSize(1));
    Map<String, TypedValue<?>> params = interception.get(0);

    assertValue(params.get("receiver"), "Malaga!");
    assertValue(params.get("prefix"), "");
    assertValue(params.get("greeting"), "good morning");
    assertValue(params.get("suffix"), null);

    assertThat(params.keySet(), not(containsInAnyOrder("nationalID", "name")));

    interception = dump.get("introduceMyself");
    assertThat(interception, hasSize(1));
    params = interception.get(0);

    assertValue(params.get("nationalID"), "5");
    assertValue(params.get("name"), "Anthony Stark");
    assertThat(params.keySet(), not(containsInAnyOrder("receiver", "prefix", "greeting", "suffix")));
  }

  @Test
  @Description("Flow vars don't make it into operations and operation vars don't exit them")
  public void variablesProperlyScoped() throws Exception {
    final Object token = new Object();
    CoreEvent result = flowRunner("composedOperation")
        .withVariable("token", token)
        .run();

    assertThat(result.getVariables().size(), is(1));
    assertValue(result.getVariables().get("token"), token);
  }

  @Test
  @Description("Message payload and attributes do not propagate into operations")
  public void messageProperlyScoped() throws Exception {
    CoreEvent result = flowRunner("salutationFlow")
        .withPayload("Hello!")
        .withAttributes(new Object())
        .run();

    assertHelloWorldResponse(result);
  }

  private void assertValue(TypedValue<?> typedValue, Object rawValue) {
    assertThat(typedValue.getValue(), equalTo(rawValue));
  }

  private void assertHelloWorldResponse(CoreEvent resultEvent) {
    assertThat(resultEvent.getMessage().getPayload().getValue(), equalTo("Hello,   Malaga! "));
    assertThat(resultEvent.getMessage().getAttributes().getValue(), is(nullValue()));
  }
}

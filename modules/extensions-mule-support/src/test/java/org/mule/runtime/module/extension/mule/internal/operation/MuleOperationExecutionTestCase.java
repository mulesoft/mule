/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.operation;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;

import java.util.List;
import java.util.Map;

import io.qameta.allure.Description;
import org.junit.Test;

public class MuleOperationExecutionTestCase extends MuleArtifactFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "mule-salutation-operations-config.xml";
  }

  @Test
  @Description("Calls a flow that executes the simple <this:hello-world> operation")
  public void executeHelloWorldOperation() throws Exception {
    CoreEvent resultEvent = flowRunner("salutationFlow").run();
    assertThat(resultEvent.getMessage().getPayload().getValue(), equalTo("Hello,  Malaga! "));
    assertThat(resultEvent.getMessage().getAttributes().getValue(), is(nullValue()));
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
  @Description("Verifies that operations params don't exit its own scope when composed")
  public void captureParamsAcrossOperations() throws Exception {
    CoreEvent resultEvent = flowRunner("interceptAndDumpParameters").run();
    Map<String, List<Map<String, TypedValue<?>>>> dump =
        (Map<String, List<Map<String, TypedValue<?>>>>) resultEvent.getMessage().getPayload().getValue();
    assertThat(dump.entrySet(), hasSize(2));

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


  private void assertValue(TypedValue<?> typedValue, Object rawValue) {
    assertThat(typedValue.getValue(), equalTo(rawValue));
  }

}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.operation;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;

import java.util.List;
import java.util.Map;

import io.qameta.allure.Description;
import org.junit.Test;

public class MuleOperationExecutionTestCase extends MuleArtifactFunctionalTestCase {

  private static List<Map<String, TypedValue<?>>> CAPTURED_PARAMS;

  public static class ParametersCaptor implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      if (CAPTURED_PARAMS != null) {
        CAPTURED_PARAMS.add(event.getParameters());
      }
      return event;
    }
  }

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
    assertThat(resultEvent.getMessage().getPayload().getValue(), equalTo("good morning  Malaga!  Hello lad, my name is Anthony Stark"));
    assertThat(resultEvent.getMessage().getAttributes().getValue(), is(nullValue()));
  }

  @Test
  public void captureParamsAcrossOperations() throws Exception {

  }
}

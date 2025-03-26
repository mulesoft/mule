/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.operation;

import static org.mule.runtime.extension.api.ExtensionConstants.STREAMING_STRATEGY_PARAMETER_NAME;
import static org.mule.test.allure.AllureConstants.ReuseFeature.REUSE;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.OPERATIONS;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.streaming.iterator.ConsumerStreamingIterator;

import java.util.List;

import jakarta.inject.Inject;

import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(REUSE)
@Story(OPERATIONS)
public class MuleOperationSupportsStreamingTestCase extends MuleArtifactFunctionalTestCase {

  @Inject
  private ExtensionManager extensionManager;

  private static List<OperationModel> operationModels;

  @Override
  protected String getConfigFile() {
    return "mule-streaming-ops-config.xml";
  }

  @Test
  @Description("Checks that an operation without streaming does not support streaming, and has no parameter for streaming strategy")
  public void withoutStreaming() {
    assertForOperation("nonStreaming");
  }

  @Test
  @Description("Checks that an operation that returns a repeatable stream does not support streaming, but has no parameter for streaming strategy")
  public void withRepeatableStreaming() {
    assertForOperation("withRepeatableStreaming");
  }

  @Test
  @Description("Checks that an operation that returns a non repeatable stream does not support streaming, but has no parameter for streaming strategy")
  public void withNonRepeatableStreaming() {
    assertForOperation("withNonRepeatableStreaming");
  }

  @Test
  @Description("Checks that an operation that returns a repeatable stream does not support streaming even if it's used, but has no parameter for streaming strategy")
  public void withRepeatableStreamingUsed() {
    assertForOperation("withRepeatableStreamingUsed");
  }

  @Test
  @Description("Checks that an operation that returns a non repeatable stream does not support streaming even if it's consumed, but has no parameter for streaming strategy")
  public void withNonRepeatableStreamingConsumed() {
    assertForOperation("withNonRepeatableStreamingConsumed");
  }

  @Test
  @Description("Check that an operation that uses an operation with repeatable streaming, but has no parameter for streaming strategy")
  public void usingStreamingOp() {
    assertForOperation("usingStreamingOp");
  }

  @Test
  @Description("Checks that return type is correct for operations that return a repeatable stream")
  public void repeatableStreamingOperationExecution() throws Exception {
    CoreEvent resultEvent = flowRunner("flowRepeatable").run();
    Object stream = resultEvent.getMessage().getPayload().getValue();
    assertThat(stream, is(instanceOf(CursorIteratorProvider.class)));
  }

  @Test
  @Description("Checks that return type is correct for operations that return a non repeatable stream")
  public void nonRepeatableStreamingOperationExecution() throws Exception {
    CoreEvent resultEvent = flowRunner("flowNonRepeatable").keepStreamsOpen().run();
    Object stream = resultEvent.getMessage().getPayload().getValue();
    assertThat(stream, is(instanceOf(ConsumerStreamingIterator.class)));

    ConsumerStreamingIterator streamingIterator = (ConsumerStreamingIterator) stream;
    assertThat(streamingIterator.hasNext(), is(true));
  }

  @Test
  @Description("Checks that return type is correct for operations that return a repeatable stream, including when used")
  public void repeatableStreamingUsedOperationExecution() throws Exception {
    CoreEvent resultEvent = flowRunner("flowRepeatableUsed").run();
    Object stream = resultEvent.getMessage().getPayload().getValue();
    assertThat(stream, is(instanceOf(CursorIteratorProvider.class)));
  }

  @Test
  @Description("Checks that return type is correct for operations that return a non repeatable stream, including when consumed")
  public void consumedNonRepeatableStreamingOperationExecution() throws Exception {
    CoreEvent resultEvent = flowRunner("flowNonRepeatableConsumed").keepStreamsOpen().run();
    Object stream = resultEvent.getMessage().getPayload().getValue();
    assertThat(stream, is(instanceOf(ConsumerStreamingIterator.class)));

    ConsumerStreamingIterator streamingIterator = (ConsumerStreamingIterator) stream;
    assertThat(streamingIterator.hasNext(), is(false));
  }

  private void assertForOperation(String operation) {
    OperationModel model = getOperationModel(operation);
    assertThat(model.supportsStreaming(), is(false));
    assertThat(model.getAllParameterModels().stream()
        .anyMatch(parameterModel -> parameterModel.getName().equals(STREAMING_STRATEGY_PARAMETER_NAME)), is(false));
  }

  private OperationModel getOperationModel(String name) {
    if (operationModels == null) {
      operationModels = extensionManager.getExtension(muleContext.getConfiguration().getId()).get().getOperationModels();
    }
    return operationModels.stream().filter(opModel -> opModel.getName().equals(name)).findFirst().get();
  }

}

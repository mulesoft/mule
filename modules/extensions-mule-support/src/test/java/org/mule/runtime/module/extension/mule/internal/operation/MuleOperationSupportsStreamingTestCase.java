/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.operation;

import static org.mule.runtime.extension.api.ExtensionConstants.STREAMING_STRATEGY_PARAMETER_NAME;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.streaming.iterator.ConsumerStreamingIterator;

import org.junit.Test;

import javax.inject.Inject;
import java.util.List;

public class MuleOperationSupportsStreamingTestCase extends MuleArtifactFunctionalTestCase {

  @Inject
  private ExtensionManager extensionManager;

  private static List<OperationModel> operationModels;

  @Override
  protected String getConfigFile() {
    return "mule-streaming-ops-config.xml";
  }

  @Test
  public void withoutStreaming() {
    assertForOpeartion("nonStreaming", false);
  }

  @Test
  public void withRepeatableStreaming() {
    assertForOpeartion("withRepeatableStreaming", true);
  }

  @Test
  public void withNonRepeatableStreaming() {
    assertForOpeartion("withNonRepeatableStreaming", true);
  }

  @Test
  public void withRepeatableStreamingUsed() {
    assertForOpeartion("withRepeatableStreamingUsed", true);
  }

  @Test
  public void withNonRepeatableStreamingConsumed() {
    assertForOpeartion("withNonRepeatableStreamingConsumed", true);
  }

  @Test
  public void repeatableStreamingOperationExecution() throws Exception {
    CoreEvent resultEvent = flowRunner("flowRepeatable").run();
    Object stream = resultEvent.getMessage().getPayload().getValue();
    assertThat(stream, is(instanceOf(CursorIteratorProvider.class)));
  }

  @Test
  public void nonRepeatableStreamingOperationExecution() throws Exception {
    CoreEvent resultEvent = flowRunner("flowNonRepeatable").keepStreamsOpen().run();
    Object stream = resultEvent.getMessage().getPayload().getValue();
    assertThat(stream, is(instanceOf(ConsumerStreamingIterator.class)));

    ConsumerStreamingIterator streamingIterator = (ConsumerStreamingIterator) stream;
    assertThat(streamingIterator.hasNext(), is(true));
  }

  @Test
  public void repeatableStreamingUsedOperationExecution() throws Exception {
    CoreEvent resultEvent = flowRunner("flowRepeatableUsed").run();
    Object stream = resultEvent.getMessage().getPayload().getValue();
    assertThat(stream, is(instanceOf(CursorIteratorProvider.class)));
  }

  @Test
  public void consumedNonRepeatableStreamingOperationExecution() throws Exception {
    CoreEvent resultEvent = flowRunner("flowNonRepeatableConsumed").keepStreamsOpen().run();
    Object stream = resultEvent.getMessage().getPayload().getValue();
    assertThat(stream, is(instanceOf(ConsumerStreamingIterator.class)));

    ConsumerStreamingIterator streamingIterator = (ConsumerStreamingIterator) stream;
    assertThat(streamingIterator.hasNext(), is(false));
  }

  private void assertForOpeartion(String operation, boolean expectedSupportStreaming) {
    OperationModel model = getOperationModel(operation);
    assertThat(model.supportsStreaming(), is(expectedSupportStreaming));
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

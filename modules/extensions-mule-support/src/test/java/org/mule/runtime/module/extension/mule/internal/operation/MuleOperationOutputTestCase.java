/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.operation;

import static org.mule.test.allure.AllureConstants.ReuseFeature.REUSE;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.OPERATIONS;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.api.streaming.StreamingStatistics;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.subtypes.extension.CarDoor;

import jakarta.inject.Inject;

import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(REUSE)
@Story(OPERATIONS)
public class MuleOperationOutputTestCase extends MuleArtifactFunctionalTestCase {

  @Inject
  private StreamingManager streamingManager;

  @Override
  protected void doTearDownAfterMuleContextDispose() {
    // The managed streams aren't going to be closed by the operation itself, but by the Ghost Buster.
    assertAllStreamingResourcesClosed();
  }

  @Override
  protected String getConfigFile() {
    return "mule-operations-with-different-outputs-config.xml";
  }

  @Test
  @Description("Executes an operation with a set-payload, but with void output type, then the output payload is null")
  public void voidOutputOperation() throws Exception {
    CoreEvent resultEvent = flowRunner("voidOutputOperationFlow").run();
    assertThat(resultEvent.getMessage().getPayload().getValue(), is(nullValue()));
  }

  @Test
  @Description("Executes an operation with a set-payload and string output type, then the output payload is the string")
  public void stringOutputOperation() throws Exception {
    CoreEvent resultEvent = flowRunner("stringOutputOperationFlow").run();
    assertThat(resultEvent.getMessage().getPayload().getValue(), is("Expected output"));
  }

  @Test
  @Description("Executes an operation setting a repeatable stream output, but with void output type, then the output payload is null")
  public void withRepeatableStreamingAndVoidOutput() throws Exception {
    CoreEvent resultEvent = flowRunner("withRepeatableStreamingAndVoidOutputFlow").keepStreamsOpen().run();
    assertThat(resultEvent.getMessage().getPayload().getValue(), is(nullValue()));
  }

  @Test
  @Description("Executes an operation setting a repeatable stream output, then the output payload is the stream")
  public void withRepeatableStreaming() throws Exception {
    CoreEvent resultEvent = flowRunner("withRepeatableStreamingFlow").keepStreamsOpen().run();
    Object cursorIteratorProvider = resultEvent.getMessage().getPayload().getValue();
    assertThat(cursorIteratorProvider, is(instanceOf(CursorIteratorProvider.class)));
  }

  @Test
  @Description("An operation declaring an output payload type belonging to another extension")
  public void returningTypeFromDependency() throws Exception {
    CoreEvent resultEvent = flowRunner("returningDoorFlow").run();
    assertThat(resultEvent.getMessage().getPayload().getDataType().getType(), is(CarDoor.class));
  }

  private void assertAllStreamingResourcesClosed() {
    if (streamingManager == null) {
      // This null check isn't needed on a happy path, but on startup failure it can raise an NPE, making the test
      // failure message unreadable.
      return;
    }
    StreamingStatistics stats = streamingManager.getStreamingStatistics();
    new PollingProber(10000L, 100L).check(new JUnitLambdaProbe(() -> {
      assertThat("There are still open cursor providers", stats.getOpenCursorProvidersCount(), is(0));
      assertThat("There are still open cursors", stats.getOpenCursorsCount(), is(0));
      return true;
    }));
  }

  @Override
  protected ExpressionLanguageMetadataService getExpressionLanguageMetadataService() {
    return new FakeExpressionLanguageMetadataService();
  }
}

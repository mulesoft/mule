/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.operation;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mule.test.allure.AllureConstants.ReuseFeature.REUSE;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.OPERATIONS;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;
import org.mule.extension.mule.testing.processing.strategies.test.api.ExecutionThreadTracker;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;

import javax.inject.Inject;

@Feature(REUSE)
@Story(OPERATIONS)
public class MuleOperationProcessingStrategyTestCase extends MuleArtifactFunctionalTestCase {

  @Inject
  private ExecutionThreadTracker executionThreadTracker;

  @Override
  protected String getConfigFile() {
    return "mule-operation-processing-strategy-test-config.xml";
  }

  @Test
  // .+===================================+......................................
  // .|..Composed.Operation...............|......................................
  // .|....+============================+.|.......+============================+.
  // .|....|.Operation..................|.|.......|.Operation..................|.
  // .|....|..-.ExecutionType=BLOCKING..|.|.......|..-.ExecutionType=CPU_LITE..|.
  // .|....|..-.CompletionCallback=NO...|.|.====>.|..-.CompletionCallback=NO...|.
  // .|....|..-.ExecutionThread=ThreadA.|.|.......|..-.ExecutionThread=ThreadB.|.
  // .|....+============================+.|.......+============================+.
  // .+===================================+......................................
  //
  // Assertion: ThreadA == ThreadB
  //
  @Description("Given a composed operation with only a BLOCKING child operation, and a CPU_LITE operation without callback, " +
      "when they are executed in that order, the BLOCKING child execution thread is the same as the CPU_LITE")
  public void blockingOperationInsideBodyDoesNotJumpThreadAfterExecute() throws Exception {
    flowRunner("blockingFlow").run();

    String blockingOpExecutionThread = getExecutionThreadName("Blocking child");
    String nextOpExecutionThread = getExecutionThreadName("After operation with one blocking child");
    assertThat(blockingOpExecutionThread, is(nextOpExecutionThread));
  }

  @Test
  // ......................................+===================================+.
  // ......................................|..Composed.Operation...............|.
  // .+============================+.......|....+============================+.|.
  // .|.Operation..................|.......|....|.Operation..................|.|.
  // .|..-.ExecutionType=CPU_LITE..|.......|....|..-.ExecutionType=BLOCKING..|.|.
  // .|..-.CompletionCallback=NO...|.====>.|....|..-.CompletionCallback=NO...|.|.
  // .|..-.ExecutionThread=ThreadA.|.......|....|..-.ExecutionThread=ThreadB.|.|.
  // .+============================+.......|....+============================+.|.
  // ......................................+===================================+.
  //
  // Assertion: ThreadA != ThreadB
  //
  @Description("Given a CPU_LITE operation without callback, and a composed operation with only a BLOCKING child " +
      "operation, when they are executed in that order, the BLOCKING child execution thread is other than the CPU_LITE")
  public void blockingOperationInsideBodyJumpsThreadBeforeExecute() throws Exception {
    flowRunner("blockingFlow").run();

    String previousOpExecutionThread = getExecutionThreadName("Before operation with one blocking child");
    String blockingOpExecutionThread = getExecutionThreadName("Blocking child");
    assertThat(blockingOpExecutionThread, is(not(previousOpExecutionThread)));
  }

  @Test
  // .+===================================+......................................
  // .|..Composed.Operation...............|......................................
  // .|....+============================+.|.......+============================+.
  // .|....|.Operation..................|.|.......|.Operation..................|.
  // .|....|..-.ExecutionType=CPU_LITE..|.|.......|..-.ExecutionType=CPU_LITE..|.
  // .|....|..-.CompletionCallback=YES..|.|.====>.|..-.CompletionCallback=NO...|.
  // .|....|..-.ExecutionThread=ThreadA.|.|.......|..-.ExecutionThread=ThreadB.|.
  // .|....+============================+.|.......+============================+.
  // .+===================================+......................................
  //
  // Assertion: ThreadA != ThreadB
  //
  @Description("Given a composed operation ended with a non-blocking operation that is completed in thread A and another" +
      "CPU_LITE operation without completion callback that executes in the thread B, when they are executed in that order" +
      "then the threads A and B are different")
  public void operationAfterANonBlockingEndedComposedOperationRunsInDifferentThread() throws Exception {
    flowRunner("nonBlockingFlow").run();

    String nonBlockingOpCompletionThread = getCompletionThreadName("Non-blocking child");
    String nextOpExecutionThread = getExecutionThreadName("After operation with one non-blocking child");
    assertThat(nextOpExecutionThread, is(not(nonBlockingOpCompletionThread)));
  }

  @Test
  // ......................................+===================================+.
  // ......................................|..Composed.Operation...............|.
  // .+============================+.......|....+============================+.|.
  // .|.Operation..................|.......|....|.Operation..................|.|.
  // .|..-.ExecutionType=CPU_LITE..|.......|....|..-.ExecutionType=CPU_LITE..|.|.
  // .|..-.CompletionCallback=NO...|.====>.|....|..-.CompletionCallback=YES..|.|.
  // .|..-.ExecutionThread=ThreadA.|.......|....|..-.ExecutionThread=ThreadB.|.|.
  // .+============================+.......|....+============================+.|.
  // ......................................+===================================+.
  //
  // Assertion: ThreadA == ThreadB
  //
  @Description("Given a CPU_LITE operation without completion callback that executes in the thread A, and a composed " +
      "operation ended with a non-blocking operation that is completed in thread B, when they are executed in that order" +
      "then the threads A and B are the same")
  public void operationBeforeANonBlockingComposedOperationRunsInSameThread() throws Exception {
    flowRunner("blockingFlow").run();

    String blockingOpExecutionThread = getExecutionThreadName("Blocking child");
    String nextOpExecutionThread = getExecutionThreadName("After operation with one blocking child");
    assertThat(blockingOpExecutionThread, is(nextOpExecutionThread));
  }

  private String getCompletionThreadName(String key) {
    return executionThreadTracker.getCompletionThreadName(key);
  }

  private String getExecutionThreadName(String key) {
    return executionThreadTracker.getExecutionThreadName(key);
  }
}

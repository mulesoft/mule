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
  // .+====================================+......................................
  // .|..Composed.Operation................|......................................
  // .|....+=============================+.|.......+============================+.
  // .|....|.Operation...................|.|.......|.Operation..................|.
  // .|....|..-.ExecutionType=BLOCKING...|.|.......|..-.ExecutionType=CPU_LITE..|.
  // .|....|..-.CompletionCallback=NO....|.|.====>.|..-.CompletionCallback=NO...|.
  // .|....|..-.ExecutionPhase=PhaseA....|.|.......|..-.ExecutionPhase=PhaseB...|.
  // .|....|..-.CompletionPhase=PhaseC...|.|.......|..-.CompletionPhase=PhaseD..|.
  // .|....+=============================+.|.......+============================+.
  // .+====================================+......................................
  //
  // Assertion: PhaseA == PhaseB
  //
  @Description("Given a composed operation with only a BLOCKING child operation, and a CPU_LITE operation without callback, " +
      "when they are executed in that order, the BLOCKING child execution thread is the same as the CPU_LITE")
  public void blockingOperationInsideBodyDoesNotJumpThreadAfterExecute() throws Exception {
    flowRunner("blockingFlow").run();

    Integer blockingOpExecutionPhase = getExecutionThreadPhase("Blocking child");
    Integer nextOpExecutionPhase = getExecutionThreadPhase("After operation with one blocking child");
    assertThat(blockingOpExecutionPhase, is(nextOpExecutionPhase));
  }

  @Test
  // ......................................+====================================+.
  // ......................................|..Composed.Operation................|.
  // .+============================+.......|....+=============================+.|.
  // .|.Operation..................|.......|....|.Operation...................|.|.
  // .|..-.ExecutionType=CPU_LITE..|.......|....|..-.ExecutionType=BLOCKING...|.|.
  // .|..-.CompletionCallback=NO...|.====>.|....|..-.CompletionCallback=NO....|.|.
  // .|..-.ExecutionPhase=PhaseA...|.......|....|..-.ExecutionPhase=PhaseB....|.|.
  // .|..-.CompletionPhase=PhaseC..|.......|....|..-.CompletionPhase=PhaseD...|.|.
  // .+============================+.......|....+=============================+.|.
  // ......................................+====================================+.
  //
  // Assertion: PhaseA != PhaseB
  //
  @Description("Given a CPU_LITE operation without callback, and a composed operation with only a BLOCKING child " +
      "operation, when they are executed in that order, the BLOCKING child execution thread is other than the CPU_LITE")
  public void blockingOperationInsideBodyJumpsThreadBeforeExecute() throws Exception {
    flowRunner("blockingFlow").run();

    Integer previousOpExecutionPhase = getExecutionThreadPhase("Before operation with one blocking child");
    Integer blockingOpExecutionPhase = getExecutionThreadPhase("Blocking child");
    assertThat(blockingOpExecutionPhase, is(not(previousOpExecutionPhase)));
  }

  @Test
  // .+====================================+......................................
  // .|..Composed.Operation................|......................................
  // .|....+=============================+.|.......+============================+.
  // .|....|.Operation...................|.|.......|.Operation..................|.
  // .|....|..-.ExecutionType=CPU_LITE...|.|.......|..-.ExecutionType=CPU_LITE..|.
  // .|....|..-.CompletionCallback=YES...|.|.====>.|..-.CompletionCallback=NO...|.
  // .|....|..-.ExecutionPhase=PhaseA....|.|.......|..-.ExecutionPhase=PhaseB...|.
  // .|....|..-.CompletionPhase=PhaseC...|.|.......|..-.CompletionPhase=PhaseD..|.
  // .|....+=============================+.|.......+============================+.
  // .+====================================+......................................
  //
  // Assertion: PhaseC != PhaseB
  //
  @Description("Given a composed operation ended with a non-blocking operation that is completed in phase C and another" +
      "CPU_LITE operation without completion callback that executes in the phase B, when they are executed in that order" +
      "then the phases C and B are different")
  public void operationAfterANonBlockingOnlyComposedOperationRunsInDifferentPhases() throws Exception {
    flowRunner("nonBlockingFlow").run();

    Integer nonBlockingOpCompletionPhase = getCompletionThreadPhase("Non-blocking child");
    Integer nextOpExecutionPhase = getExecutionThreadPhase("After operation with one non-blocking child");
    assertThat(nextOpExecutionPhase, is(not(nonBlockingOpCompletionPhase)));
  }

  @Test
  // ......................................+====================================+.
  // ......................................|..Composed.Operation................|.
  // .+============================+.......|....+=============================+.|.
  // .|.Operation..................|.......|....|.Operation...................|.|.
  // .|..-.ExecutionType=CPU_LITE..|.......|....|..-.ExecutionType=CPU_LITE...|.|.
  // .|..-.CompletionCallback=NO...|.====>.|....|..-.CompletionCallback=YES...|.|.
  // .|..-.ExecutionPhase=PhaseA...|.......|....|..-.ExecutionPhase=PhaseB....|.|.
  // .|..-.CompletionPhase=PhaseC..|.......|....|..-.CompletionPhase=PhaseD...|.|.
  // .+============================+.......|....+=============================+.|.
  // ......................................+====================================+.
  //
  // Assertion: PhaseA == PhaseB
  //
  @Description("Given a CPU_LITE operation without completion callback that executes in the phase A, and a composed " +
      "operation ended with a non-blocking operation that is executed in phase B, when they are executed in that order" +
      "then the phases A and B are the same")
  public void operationBeforeANonBlockingComposedOperationRunsInSameThread() throws Exception {
    flowRunner("blockingFlow").run();

    Integer blockingOpExecutionPhase = getExecutionThreadPhase("Blocking child");
    Integer nextOpExecutionPhase = getExecutionThreadPhase("After operation with one blocking child");
    assertThat(blockingOpExecutionPhase, is(nextOpExecutionPhase));
  }


  @Test
  // .....................................+=====================================================================+.
  // .....................................|.Composed.Operation..................................................|.
  // .+===========================+.......|...+============================+.....+============================+.|.
  // .|.Operation.................|.......|...|.Operation..................|.....|.Operation..................|.|.
  // .|..-.ExecutionType=CPU_LITE.|.......|...|..-.ExecutionType=BLOCKING..|.....|..-.ExecutionType=CPU_LITE..|.|.
  // .|..-.CompletionCallback=NO..|.====>.|...|..-.CompletionCallback=NO...|.==>.|..-.CompletionCallback=NO...|.|.
  // .|..-.ExecutionPhase=PhaseA..|.......|...|..-.ExecutionPhase=PhaseB...|.....|..-.ExecutionPhase=PhaseC...|.|.
  // .|..-.CompletionPhase=PhaseD.|.......|...|..-.CompletionPhase=PhaseE..|.....|..-.CompletionPhase=PhaseF..|.|.
  // .+===========================+.......|...+============================+.....+============================+.|.
  // .....................................+=====================================================================+.
  //
  // Assertion: PhaseA != PhaseB
  //
  public void blockingOperationInsideComposedBodyJumpsThreadBeforeExecute() throws Exception {
    flowRunner("blockingComposedFlow").run();

    Integer previousOpExecutionPhase = getExecutionThreadPhase("Before operationWithOneBlockingAndOneCpuLiteChildren operation");
    Integer blockingOpExecutionPhase = getExecutionThreadPhase("Blocking child");
    assertThat(blockingOpExecutionPhase, is(not(previousOpExecutionPhase)));
  }

  @Test
  // .+=====================================================================+.....................................
  // .|.Composed.Operation..................................................|.....................................
  // .|...+============================+.....+============================+.|.......+===========================+.
  // .|...|.Operation..................|.....|.Operation..................|.|.......|.Operation.................|.
  // .|...|..-.ExecutionType=CPU_LITE..|.....|..-.ExecutionType=CPU_LITE..|.|.......|..-.ExecutionType=CPU_LITE.|.
  // .|...|..-.CompletionCallback=NO...|.==>.|..-.CompletionCallback=YES..|.|.====>.|..-.CompletionCallback=NO..|.
  // .|...|..-.ExecutionPhase=PhaseA...|.....|..-.ExecutionPhase=PhaseB...|.|.......|..-.ExecutionPhase=PhaseC..|.
  // .|...|..-.CompletionPhase=PhaseD..|.....|..-.CompletionPhase=PhaseE..|.|.......|..-.CompletionPhase=PhaseF.|.
  // .|...+============================+.....+============================+.|.......+===========================+.
  // .+=====================================================================+.....................................
  //
  // Assertion: PhaseE != PhaseC
  //
  public void operationAfterANonBlockingEndedComposedOperationRunsInDifferentPhases() throws Exception {
    flowRunner("nonBlockingComposedFlow").run();

    Integer nonBlockingOpCompletionPhase = getCompletionThreadPhase("Non-blocking child");
    Integer nextOpExecutionPhase = getExecutionThreadPhase("After operationWithOneCpuLiteAndOneNonBlockingChildren operation");
    assertThat(nextOpExecutionPhase, is(not(nonBlockingOpCompletionPhase)));
  }

  private Integer getCompletionThreadPhase(String key) {
    return executionThreadTracker.getCompletionThreadPhase(key);
  }

  private Integer getExecutionThreadPhase(String key) {
    return executionThreadTracker.getExecutionThreadPhase(key);
  }
}

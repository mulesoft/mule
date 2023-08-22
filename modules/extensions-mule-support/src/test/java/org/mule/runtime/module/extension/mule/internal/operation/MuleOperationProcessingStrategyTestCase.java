/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.operation;

import static org.mule.test.allure.AllureConstants.ReuseFeature.REUSE;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.OPERATIONS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import org.mule.extension.mule.testing.processing.strategies.test.api.ExecutionThreadTracker;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;

import javax.inject.Inject;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

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
    assertThat(executionPhaseForKey("Blocking child"), is(executionPhaseForKey("After operation with one blocking child")));
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
    assertThat(executionPhaseForKey("Blocking child"), is(not(executionPhaseForKey("Before operation with one blocking child"))));
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
    assertThat(executionPhaseForKey("After operation with one non-blocking child"),
               is(not(completionPhaseForKey("Non-blocking child"))));
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
    assertThat(executionPhaseForKey("Blocking child"), is(executionPhaseForKey("After operation with one blocking child")));
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
    assertThat(executionPhaseForKey("Blocking child"),
               is(not(executionPhaseForKey("Before operationWithOneBlockingAndOneCpuLiteChildren operation"))));
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
    assertThat(executionPhaseForKey("After operationWithOneCpuLiteAndOneNonBlockingChildren operation"),
               is(not(completionPhaseForKey("Non-blocking child"))));
  }

  private Integer completionPhaseForKey(String key) {
    return executionThreadTracker.getCompletionThreadPhase(key);
  }

  private Integer executionPhaseForKey(String key) {
    return executionThreadTracker.getExecutionThreadPhase(key);
  }
}

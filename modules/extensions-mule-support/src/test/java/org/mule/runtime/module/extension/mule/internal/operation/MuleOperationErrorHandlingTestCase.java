/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.operation;

import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import static org.mule.tck.junit4.matcher.ErrorTypeMatcher.errorType;
import static org.mule.tck.junit4.matcher.EventMatcher.hasMessage;
import static org.mule.test.allure.AllureConstants.ReuseFeature.REUSE;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.OPERATIONS;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.component.execution.ComponentExecutionException;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.exception.EventProcessingException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(REUSE)
@Stories({@Story(OPERATIONS), @Story(ERROR_HANDLING)})
public class MuleOperationErrorHandlingTestCase extends MuleArtifactFunctionalTestCase {

  @Override
  protected String[] getConfigFiles() {
    return new String[] {
        "mule-error-handling-operations-config.xml",
        "mule-error-handling-with-try-operations-config.xml",
        "reusing-error-handling-mule-config.xml"
    };
  }

  @Test
  public void errorWithinAppWithNamespaceThis() throws Exception {
    flowRunner("raiseErrorWithinThisNamespace").runExpectingException(errorType("THIS", "CUSTOM"));
  }

  @Test
  public void divisionByZero() throws Exception {
    flowRunner("divisionByZeroFlow").runExpectingException(errorType("MULE", "EXPRESSION"));
  }

  @Test
  public void heisenbergCureCancer() throws Exception {
    flowRunner("heisenbergCureCancerFlow").runExpectingException(errorType("HEISENBERG", "HEALTH"));
  }

  @Test
  public void usingOperationRaiseError() throws Exception {
    flowRunner("usingOperationRaiseErrorFlow").runExpectingException(errorType("THIS", "CUSTOM"));
  }

  @Test
  public void errorMappingInInvocation() throws Exception {
    flowRunner("errorMappingOnInvocationFlow").runExpectingException(errorType("MY", "MAPPED"));
  }

  @Test
  public void errorMappingInsideBody() throws Exception {
    flowRunner("errorMappingInsideBodyFlow").runExpectingException(errorType("MY", "MAPPED"));
  }

  @Test
  public void errorMappingInsideBodyAndInInvocation() throws Exception {
    flowRunner("errorMappingInsideBodyAndInInvocationFlow").runExpectingException(errorType("MY", "MAPPED_TWICE"));
  }

  @Test
  public void callingOperationThatSilencesErrors() throws Exception {
    flowRunner("flowCallingOperationThatSilencesOneSpecificErrorAndRaisesAnother")
        .runExpectingException(errorType("THIS", "CUSTOM"));
  }

  @Test
  public void executionExceptionHasTheCaughtErrorAsCause() throws Exception {
    try {
      flowRunner("flowCallingOperationThatSilencesOneSpecificErrorAndRaisesAnother").run();
      fail("Calling the flow should have failed");
    } catch (EventProcessingException exception) {
      Optional<Error> optionalError = exception.getEvent().getError();
      assertThat(optionalError.isPresent(), is(true));

      Error error = optionalError.get();
      assertThat(error.getErrorType(), errorType("THIS", "CUSTOM"));

      Throwable executionException = error.getCause();
      assertThat(executionException, instanceOf(ComponentExecutionException.class));

      Throwable causeAsException = executionException.getCause();
      assertThat(causeAsException, instanceOf(Error.class));

      Error causeAsError = (Error) causeAsException;
      assertThat(causeAsError.getErrorType(), errorType("HEISENBERG", "HEALTH"));
    }
  }

  @Test
  public void reusableErrorHandlerAsAnOperation() throws Exception {
    CoreEvent result = flowRunner("reusableErrorHandlerAsAnOperationFlow").run();
    assertThat(result, hasMessage(hasPayload(is("Caught error!"))));
  }

  @Test
  public void errorMappingIsNotTransitive() throws Exception {
    flowRunner("transitiveMappingFlow").runExpectingException(errorType("MY", "MAPPED"));
  }

  @Test
  public void mappingChildAfterParent() throws Exception {
    flowRunner("mappingChildAfterParentFlow").runExpectingException(errorType("MY", "MAPPEDCONNECTIVITY"));
  }

  @Test
  public void nestedTriesWithDefaultCauses() throws Exception {
    try {
      flowRunner("nestedErrorsFlow").run();
      fail("Calling the flow should have failed");
    } catch (EventProcessingException exception) {
      Optional<Error> optionalError = exception.getEvent().getError();
      assertThat(optionalError.isPresent(), is(true));
      Error error = optionalError.get();
      assertThat(error.getErrorType(), errorType("THIS", "FOURTH"));

      Error causeOfFourth = getErrorCause(error);
      assertThat(causeOfFourth, is(notNullValue()));
      assertThat(causeOfFourth.getErrorType(), errorType("THIS", "THIRD"));

      Error causeOfThird = getErrorCause(causeOfFourth);
      assertThat(causeOfThird, is(notNullValue()));
      assertThat(causeOfThird.getErrorType(), errorType("THIS", "SECOND"));

      Error causeOfSecond = getErrorCause(causeOfThird);
      assertThat(causeOfSecond, is(notNullValue()));
      assertThat(causeOfSecond.getErrorType(), errorType("THIS", "FIRST"));

      Throwable causeOfFirst = causeOfSecond.getCause();
      assertThat(causeOfFirst, instanceOf(DefaultMuleException.class));
    }
  }

  @Test
  public void operationReusingErrorHandlingLogic() throws Exception {
    // This just doesn't throw any error
    flowRunner("reusableErrorHandlerAsAnOperationFlow").run();
  }

  @Test
  public void tryHandlingUnknownError() throws Exception {
    try {
      flowRunner("tryHandlingUnknownErrorFlow").run();
      fail("Calling the flow should have failed");
    } catch (EventProcessingException exception) {
      Optional<Error> optionalError = exception.getEvent().getError();
      assertThat(optionalError.isPresent(), is(true));
      Error error = optionalError.get();
      assertThat(error.getErrorType(), errorType("THIS", "UNKNOWN"));

      Error cause = getErrorCause(error);
      assertThat(cause, is(notNullValue()));
      assertThat(cause.getErrorType(), errorType("THIS", "CUSTOM"));
    }
  }

  private static Error getErrorCause(Error error) {
    Set<Throwable> seen = new HashSet<>();
    Throwable currentCause = error.getCause();
    while (!seen.contains(currentCause)) {
      seen.add(currentCause);
      if (currentCause instanceof Error) {
        return (Error) currentCause;
      }
      // The exceptions we skip are all ComponentExecutionException's
      assertThat(currentCause, instanceOf(ComponentExecutionException.class));
      currentCause = currentCause.getCause();
    }
    return null;
  }

  @Test
  @Description("When an operation raises an error, the payload remains unchanged")
  public void whenAnOperationRaisesAnErrorThePayloadIsNotChanged() throws Exception {
    CoreEvent result = flowRunner("operationSettingPayloadAndRaisingErrorFlow").run();
    assertThat(result, hasMessage(hasPayload(is("Payload before calling the operation"))));
  }

  @Test
  @Description("This test is intended to avoid a change breaking backwards for the old behavior of flows (opposite to the operation's behavior)")
  public void whenAReferencedFlowChangesThePayloadAndRaisesAnErrorThePayloadIsChanged() throws Exception {
    CoreEvent result = flowRunner("backwardsCompatibleAwfulBehaviorFlow").run();
    assertThat(result, hasMessage(hasPayload(is("Payload set within the referenced flow"))));
  }
}

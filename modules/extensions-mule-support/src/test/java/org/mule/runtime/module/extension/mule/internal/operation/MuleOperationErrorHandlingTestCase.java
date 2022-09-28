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
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.component.execution.ComponentExecutionException;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.exception.EventProcessingException;

import java.util.Optional;

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
  public void nestedErrors() throws Exception {
    try {
      flowRunner("nestedErrorsFlow").run();
      fail("Calling the flow should have failed");
    } catch (EventProcessingException exception) {
      Optional<Error> optionalError = exception.getEvent().getError();
      assertThat(optionalError.isPresent(), is(true));
      Error error = optionalError.get();
      assertThat(error.getErrorType(), errorType("THIS", "FOURTH"));
      Throwable executionException = error.getCause();
      assertThat(executionException, instanceOf(ComponentExecutionException.class));

      Throwable causeOfFourth = executionException.getCause();
      assertThat(causeOfFourth, instanceOf(Error.class));
      Error causeOfFourthAsError = (Error) causeOfFourth;
      assertThat(causeOfFourthAsError.getErrorType(), errorType("THIS", "THIRD"));

      Throwable causeOfThird = causeOfFourthAsError.getCause();
      assertThat(causeOfThird, instanceOf(Error.class));
      Error causeOfThirdAsError = (Error) causeOfThird;
      assertThat(causeOfThirdAsError.getErrorType(), errorType("THIS", "SECOND"));

      Throwable causeOfSecond = causeOfThirdAsError.getCause();
      assertThat(causeOfSecond, instanceOf(Error.class));
      Error causeOfSecondAsError = (Error) causeOfSecond;
      assertThat(causeOfSecondAsError.getErrorType(), errorType("THIS", "FIRST"));

      Throwable causeOfFirst = causeOfSecondAsError.getCause();
      assertThat(causeOfFirst, instanceOf(DefaultMuleException.class));
    }
  }
}

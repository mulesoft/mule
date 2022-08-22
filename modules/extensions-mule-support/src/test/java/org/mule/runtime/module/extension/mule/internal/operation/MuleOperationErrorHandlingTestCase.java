/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.operation;

import static org.mule.tck.junit4.matcher.ErrorTypeMatcher.errorType;
import static org.mule.test.allure.AllureConstants.ReuseFeature.REUSE;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.OPERATIONS;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;

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
        "mule-error-handling-with-try-operations-config.xml"
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
}

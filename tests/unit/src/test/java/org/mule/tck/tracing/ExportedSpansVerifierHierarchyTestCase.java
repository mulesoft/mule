/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.tracing;

import static org.mule.tck.tracing.ExportedSpansVerifier.getExportedSpansVerifier;
import static org.junit.Assert.fail;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.privileged.profiling.ExportedSpanCapturer;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import org.junit.Test;

@RunWith(Parameterized.class)
public class ExportedSpansVerifierHierarchyTestCase extends AbstractExportedSpansVerifierTestCase {

  private final boolean removeParentFromLeave;
  private final boolean incorrectParentInLeave;
  private final String expectedErrorMessage;

  @Parameterized.Parameters(name = "removeParentFromLeave: {0}, incorrectParentInLeave: {1}")
  public static Collection<Object[]> modeParameters() {
    return Arrays.asList(new Object[][] {
        {false, true, "The expected parent span is wrong for span childOfChild2: expected: child1, actual: root"},
        {true, false, "The span childOfChild2 has no parent"},
        {false, false, null}
    });
  }


  public ExportedSpansVerifierHierarchyTestCase(boolean removeParentFromLeave,
                                                boolean incorrectParentInLeave,
                                                String expectedErrorMessage) {
    this.removeParentFromLeave = removeParentFromLeave;
    this.incorrectParentInLeave = incorrectParentInLeave;
    this.expectedErrorMessage = expectedErrorMessage;
  }

  @Test
  public void hierarchyFailWhenSomeExpectedLeavesDoNotHaveFather() {
    assertExpectedSpans(removeParentFromLeave, incorrectParentInLeave, expectedErrorMessage);
  }

  private void assertExpectedSpans(boolean removeParentFromLeave, boolean incorrectParentInLeave, String expectedErrorMessage) {
    try {
      ExportedSpanCapturer mockedExportedSpanCapturer =
          getExportedSpanCapturer(removeParentFromLeave, incorrectParentInLeave, new HashMap<>(), new HashMap<>(),
                                  new HashMap<>(),
                                  new HashMap<>());
      getExportedSpansVerifier("root")
          .withChildExportedSpan(getExportedSpansVerifier("child1"))
          .withChildExportedSpan(getExportedSpansVerifier("child2")
              .withChildExportedSpan(getExportedSpansVerifier("childOfChild2")))
          .verify(mockedExportedSpanCapturer);

      if (expectedErrorMessage != null) {
        fail("No Exception Thrown. Expected message: " + expectedErrorMessage);
      }
    } catch (Throwable e) {
      assertThat(e, instanceOf(AssertionError.class));
      if (expectedErrorMessage != null) {
        assertThat(e.getMessage(), equalTo(expectedErrorMessage));
      }
    }
  }
}

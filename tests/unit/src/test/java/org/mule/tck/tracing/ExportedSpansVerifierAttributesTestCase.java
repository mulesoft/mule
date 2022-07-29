/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.tracing;

import static org.mule.tck.tracing.ExportedSpansVerifier.getExportedSpansVerifier;

import static java.util.Collections.emptyMap;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.mule.runtime.core.privileged.profiling.ExportedSpanCapturer;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.Test;

@RunWith(Parameterized.class)
public class ExportedSpansVerifierAttributesTestCase extends AbstractExportedSpansVerifierTestCase {

  public static final String NO_EXCEPTION_THROWN = "Expected exception and no exception thrown";

  private final Map<String, String> attributesInRoot;
  private final Map<String, String> attributesInChild1;
  private final Map<String, String> attributesInChild2;
  private final Map<String, String> attributesInChildOfChild2;
  private final boolean verifyOnlyExistence;
  private final String expectedErrorMessage;

  @Parameterized.Parameters(
      name = "attributesInRoot: {0}, attributesInChild1: {1}, attributesInChild2: {2}, attributesInChildOfChild2: {3}, verifyOnlyExistence: {4}")
  public static Collection<Object[]> modeParameters() {
    return Arrays.asList(new Object[][] {
        {of("attribute1", "value1"), of("attribute2", "value2"), of("attribute3", "value3"), of("attribute4", "value4"), false,
            null},
        {emptyMap(), of("attribute2", "value2"), of("attribute3", "value3"), of("attribute4", "value4"), true,
            "The attribute attribute1 is not present in span root"},
        {of("attribute1", "value1"), emptyMap(), of("attribute3", "value3"), of("attribute4", "value4"), true,
            "The attribute attribute2 is not present in span child1"},
        {of("attribute1", "value1"), of("attribute2", "value2"), emptyMap(), of("attribute4", "value4"), true,
            "The attribute attribute3 is not present in span child2"},
        {of("attribute1", "value1"), of("attribute2", "value2"), of("attribute3", "value3"), emptyMap(), true,
            "The attribute attribute4 is not present in span childOfChild2"},
        {of("attribute1", "value1"), of("attribute2", "value2"), of("attribute3", "value3"), of("attribute4", "value4"), false,
            null},
        {emptyMap(), of("attribute2", "value2"), of("attribute3", "value3"), of("attribute4", "value4"), false,
            "The attribute attribute1 with value value1 is not present in span root"},
        {of("attribute1", "value1"), emptyMap(), of("attribute3", "value3"), of("attribute4", "value4"), false,
            "The attribute attribute2 with value value2 is not present in span child1"},
        {of("attribute1", "value1"), of("attribute2", "value2"), emptyMap(), of("attribute4", "value4"), false,
            "The attribute attribute3 with value value3 is not present in span child2"},
        {of("attribute1", "value1"), of("attribute2", "value2"), of("attribute3", "value3"), emptyMap(), false,
            "The attribute attribute4 with value value4 is not present in span childOfChild2"},
    });
  }

  public ExportedSpansVerifierAttributesTestCase(Map<String, String> attributesInRoot,
                                                 Map<String, String> attributesInChild1,
                                                 Map<String, String> attributesInChild2,
                                                 Map<String, String> attributesInChildOfChild2,
                                                 boolean verifyOnlyExistence,
                                                 String expectedErrorMessage) {
    this.attributesInRoot = attributesInRoot;
    this.attributesInChild1 = attributesInChild1;
    this.attributesInChild2 = attributesInChild2;
    this.attributesInChildOfChild2 = attributesInChildOfChild2;
    this.verifyOnlyExistence = verifyOnlyExistence;
    this.expectedErrorMessage = expectedErrorMessage;
  }

  @Test
  public void expectAttribute4NotPresent() {
    try {
      ExportedSpanCapturer mockedExportedSpanCapturer =
          getExportedSpanCapturer(false, false, attributesInRoot,
                                  attributesInChild1,
                                  attributesInChild2,
                                  attributesInChildOfChild2);

      if (verifyOnlyExistence) {
        verifySpanHierarchyWithHasAttribute(mockedExportedSpanCapturer);
      } else {
        verifySpanHierarchyWithAttributes(mockedExportedSpanCapturer);
      }

      if (expectedErrorMessage != null) {
        fail(NO_EXCEPTION_THROWN);
      }

    } catch (Throwable e) {
      assertThat(e, instanceOf(AssertionError.class));
      assertThat(e.getMessage(), equalTo(expectedErrorMessage));
      return;
    }
  }


  @Test
  public void expectHasAttribtueAttributeRootNotPresent() {
    try {
      ExportedSpanCapturer mockedExportedSpanCapturer =
          getExportedSpanCapturer(false, false, new HashMap<>(),
                                  of("attribute2", "value2"),
                                  of("attribute3", "value3"),
                                  of("attribute4", "value4"));

      verifySpanHierarchyWithAttributes(mockedExportedSpanCapturer);

      if (expectedErrorMessage != null) {
        fail(NO_EXCEPTION_THROWN);
      }
    } catch (Throwable e) {
      assertThat(e, instanceOf(AssertionError.class));
      assertThat(e.getMessage(), equalTo("The attribute attribute1 with value value1 is not present in span root"));
    }
  }

  private void verifySpanHierarchyWithAttributes(ExportedSpanCapturer mockedExportedSpanCapturer) {
    getExportedSpansVerifier("root")
        .withAttribute("attribute1", "value1")
        .withChildExportedSpan(getExportedSpansVerifier("child1")
            .withAttribute("attribute2", "value2"))
        .withChildExportedSpan(getExportedSpansVerifier("child2")
            .withAttribute("attribute3", "value3")
            .withChildExportedSpan(getExportedSpansVerifier("childOfChild2")
                .withAttribute("attribute4", "value4")))
        .verify(mockedExportedSpanCapturer);
  }

  private void verifySpanHierarchyWithHasAttribute(ExportedSpanCapturer mockedExportedSpanCapturer) {
    getExportedSpansVerifier("root")
        .hasAttribute("attribute1")
        .withChildExportedSpan(getExportedSpansVerifier("child1")
            .hasAttribute("attribute2"))
        .withChildExportedSpan(getExportedSpansVerifier("child2")
            .hasAttribute("attribute3")
            .withChildExportedSpan(getExportedSpansVerifier("childOfChild2")
                .hasAttribute("attribute4")))
        .verify(mockedExportedSpanCapturer);
  }
}

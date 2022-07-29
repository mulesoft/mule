/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.tracing;

import static com.google.common.collect.ImmutableList.of;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.privileged.profiling.CapturedExportedSpan;
import org.mule.runtime.core.privileged.profiling.ExportedSpanCapturer;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;

/**
 * Tests for {@link ExportedSpansVerifier}.
 *
 * @since 4.5.0
 */
public class AbstractExportedSpansVerifierTestCase {

  @NotNull
  protected ExportedSpanCapturer getExportedSpanCapturer(boolean removeParentFromLeave,
                                                         boolean incorrectParentInLeave,
                                                         Map<String, String> expectedAttributesForRoot,
                                                         Map<String, String> expectedAttributesForChild1,
                                                         Map<String, String> expectedAttributesForChild2,
                                                         Map<String, String> expectedAttributesForChildOfChild2) {
    ExportedSpanCapturer mockedExportedSpanCapturer = mock(ExportedSpanCapturer.class);
    Collection<CapturedExportedSpan> collectedSpans = new HashSet<>();

    CapturedExportedSpan root = mock(CapturedExportedSpan.class);
    CapturedExportedSpan child1 = mock(CapturedExportedSpan.class);
    CapturedExportedSpan child2 = mock(CapturedExportedSpan.class);
    CapturedExportedSpan childOfChild2 = mock(CapturedExportedSpan.class);

    when(root.getName()).thenReturn("root");
    when(root.getSpanId()).thenReturn("root");
    when(root.getChildren()).thenReturn(of(child1, child2));
    when(root.getAttributes()).thenReturn(expectedAttributesForRoot);

    when(child1.getName()).thenReturn("child1");
    when(child1.getSpanId()).thenReturn("child1");
    when(child1.getParent()).thenReturn(Optional.of(root));
    when(child1.getAttributes()).thenReturn(expectedAttributesForChild1);
    when(child2.getName()).thenReturn("child2");
    when(child2.getSpanId()).thenReturn("child1");
    when(child2.getParent()).thenReturn(Optional.of(root));
    when(child2.getChildren()).thenReturn(of(childOfChild2));
    when(child2.getAttributes()).thenReturn(expectedAttributesForChild2);

    when(childOfChild2.getName()).thenReturn("childOfChild2");
    when(childOfChild2.getSpanId()).thenReturn("childOfChild2");
    when(childOfChild2.getAttributes()).thenReturn(expectedAttributesForChildOfChild2);

    if (incorrectParentInLeave) {
      when(childOfChild2.getParent()).thenReturn(Optional.of(root));
    }

    if (!incorrectParentInLeave && !removeParentFromLeave) {
      when(childOfChild2.getParent()).thenReturn(Optional.of(child2));
    }

    collectedSpans.add(root);
    collectedSpans.add(child1);
    collectedSpans.add(child2);
    collectedSpans.add(childOfChild2);

    when(mockedExportedSpanCapturer.getExportedSpans()).thenReturn(collectedSpans);
    return mockedExportedSpanCapturer;
  }

}

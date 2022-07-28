/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4.rule;

import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.privileged.profiling.CapturedExportedSpan;
import org.mule.runtime.core.privileged.profiling.ExportedSpanCapturer;
import org.mule.runtime.core.privileged.profiling.PrivilegedProfilingService;

import java.util.Collection;

import javax.inject.Inject;

import org.junit.rules.ExternalResource;

/**
 * A Test Capturer for Exported Spans.
 */
public class TestExportedSpanCapturer extends ExternalResource implements ExportedSpanCapturer, Initialisable {

  @Inject
  PrivilegedProfilingService privilegedProfilingService;

  private ExportedSpanCapturer exportedSpanCapturer;

  @Override protected void after() {
    dispose();
  }

  @Override public void dispose() {
    exportedSpanCapturer.dispose();
  }

  @Override public Collection<CapturedExportedSpan> getExportedSpans() {
    return exportedSpanCapturer.getExportedSpans();
  }

  @Override public void initialise() throws InitialisationException {
    if (exportedSpanCapturer == null) {
      exportedSpanCapturer = privilegedProfilingService.getSpanExportManager().getExportedSpanCapturer();
    }
  }
}

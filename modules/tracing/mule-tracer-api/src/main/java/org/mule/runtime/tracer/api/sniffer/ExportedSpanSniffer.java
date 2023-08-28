/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.api.sniffer;

import org.mule.runtime.api.lifecycle.Disposable;

import java.util.Collection;

/**
 * A resource that allows to capture the exported spans. This is used only for testing purposes and is not exposed as a general
 * API.
 *
 * @since 4.5.0
 */
public interface ExportedSpanSniffer extends Disposable {

  /**
   * @return the exportd {@link CapturedExportedSpan} sniffed.
   */
  Collection<CapturedExportedSpan> getExportedSpans();

}

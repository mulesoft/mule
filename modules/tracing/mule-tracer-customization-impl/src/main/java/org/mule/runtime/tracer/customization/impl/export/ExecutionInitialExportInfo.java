/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.customization.impl.export;

import org.mule.runtime.tracer.api.span.info.InitialExportInfo;

import java.util.Set;

/**
 * An implementation of {@link InitialExportInfo} for the execution. It manages the tracing level export information necessary and
 * can propagate the export information from its parent's InitialExportInfo.
 *
 * @since 4.5.0
 */
public class ExecutionInitialExportInfo implements InitialExportInfo {

  private final TracingLevelExportInfo tracingLevelExportInfo;

  public ExecutionInitialExportInfo(TracingLevelExportInfo tracingLevelExportInfo) {
    this.tracingLevelExportInfo = tracingLevelExportInfo;
  }

  @Override
  public boolean isExportable() {
    return this.tracingLevelExportInfo.isExportable();
  }

  @Override
  public Set<String> noExportUntil() {
    return this.tracingLevelExportInfo.noExportUntil();
  }

  @Override
  public void propagateInitialExportInfo(InitialExportInfo parentInitialExportInfo) {
    ExecutionInitialExportInfo parentExecutionInitialExportInfo = ((ExecutionInitialExportInfo) parentInitialExportInfo);
    this.tracingLevelExportInfo.propagateExportInfo(parentExecutionInitialExportInfo.getTracingLevelExportInfo());
  }

  public TracingLevelExportInfo getTracingLevelExportInfo() {
    return this.tracingLevelExportInfo;
  }

}

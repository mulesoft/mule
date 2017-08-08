/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime;

import static org.mule.runtime.api.meta.model.operation.ExecutionType.BLOCKING;
import static org.mule.runtime.api.meta.model.operation.ExecutionType.CPU_INTENSIVE;
import static org.mule.runtime.api.meta.model.operation.ExecutionType.CPU_LITE;
import org.mule.runtime.api.meta.model.operation.ExecutionType;
import org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType;

public final class ExecutionTypeMapper {

  private ExecutionTypeMapper() {}

  public static ProcessingType asProcessingType(ExecutionType executionType) {
    if (executionType == CPU_LITE) {
      return ProcessingType.CPU_LITE;
    } else if (executionType == BLOCKING) {
      return ProcessingType.BLOCKING;
    } else if (executionType == CPU_INTENSIVE) {
      return ProcessingType.CPU_INTENSIVE;
    } else {
      throw new IllegalArgumentException("Unsupported executionType " + executionType);
    }
  }
}

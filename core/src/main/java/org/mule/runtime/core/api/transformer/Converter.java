/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.transformer;

import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.IO_RW;

/**
 * Defines a {@link Transformer} that is a data type converters, ie: convert data from a type to another without modifying the
 * meaning of the data.
 */
public interface Converter extends Transformer {

  int MAX_PRIORITY_WEIGHTING = 10;
  int MIN_PRIORITY_WEIGHTING = 1;
  int DEFAULT_PRIORITY_WEIGHTING = MIN_PRIORITY_WEIGHTING;

  /**
   * If two or more discoverable transformers are equal, this value can be used to select the correct one
   *
   * @return the priority weighting for this transformer. This is a value between {@link #MIN_PRIORITY_WEIGHTING} and
   *         {@link #MAX_PRIORITY_WEIGHTING}.
   */
  int getPriorityWeighting();

  /**
   * If 2 or more discoverable transformers are equal, this value can be used to select the correct one
   *
   * @param weighting the priority weighting for this transformer. This is a value between {@link #MIN_PRIORITY_WEIGHTING} and
   *        {@link #MAX_PRIORITY_WEIGHTING}.
   */
  void setPriorityWeighting(int weighting);

  @Override
  default ProcessingType getProcessingType() {
    if (getReturnDataType().isStreamType()
        || getSourceDataTypes().stream().filter(dataType -> !dataType.isStreamType()).count() > 0) {
      return IO_RW;
    } else {
      return CPU_LITE;
    }
  }

}

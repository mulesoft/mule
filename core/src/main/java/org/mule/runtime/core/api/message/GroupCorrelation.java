/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.message;

import static java.util.Optional.ofNullable;

import org.mule.runtime.api.message.Message;

import java.io.Serializable;
import java.util.Optional;

/**
 * Immutable container for correlation properties relative to a {@link Message}.
 * 
 * @since 4.0
 */
public class GroupCorrelation implements Serializable {

  private static final long serialVersionUID = -5687080761804624442L;

  public static final String NOT_SET = "<not set>";
  public static final GroupCorrelation NO_CORRELATION = new GroupCorrelation(null, null);

  private final Integer groupSize;
  private final Integer sequence;

  /**
   * Builds a new {@link GroupCorrelation} with the given parameters.
   * 
   * @param groupSize see {@link #getGroupSize()}.
   * @param sequence see {@link #getSequence()}.
   */
  public GroupCorrelation(Integer groupSize, Integer sequence) {
    this.groupSize = groupSize;
    this.sequence = sequence;
  }

  /**
   * Gets the sequence or ordering number for this message in the the correlation group.
   *
   * @return the sequence number or null value if the sequence is not important
   */
  public Optional<Integer> getSequence() {
    return ofNullable(sequence);
  }

  /**
   * Determines how many messages are in the correlation group
   *
   * @return total messages in this group or null value if the size is not known
   */
  public Optional<Integer> getGroupSize() {
    return ofNullable(groupSize);
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(120);

    // format message for multi-line output, single-line is not readable
    buf.append("{");
    buf.append("; groupSize=").append(getGroupSize().map(v -> v.toString()).orElse(NOT_SET));
    buf.append("; sequence=").append(getSequence().map(v -> v.toString()).orElse(NOT_SET));
    buf.append('}');
    return buf.toString();
  }
}

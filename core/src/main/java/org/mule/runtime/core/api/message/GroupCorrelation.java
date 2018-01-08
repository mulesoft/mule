/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.message;

import static java.util.OptionalInt.empty;

import org.mule.runtime.api.message.Message;

import java.io.Serializable;
import java.util.OptionalInt;

/**
 * Immutable container for correlation properties relative to a {@link Message}.
 * 
 * @since 4.0
 */
public class GroupCorrelation implements Serializable {

  private static final long serialVersionUID = -5687080761804624442L;
  public static final String NOT_SET = "<not set>";

  private final int sequence;
  private final int groupSize;

  public static GroupCorrelation of(int sequence) {
    return new GroupCorrelation(sequence, empty());
  }

  public static GroupCorrelation of(int sequence, int groupSize) {
    return new GroupCorrelation(sequence, OptionalInt.of(groupSize));
  }

  /**
   * Builds a new {@link GroupCorrelation} with the given parameters.
   *
   * @param sequence see {@link #getSequence()}.
   * @param groupSize see {@link #getGroupSize()}.
   */
  private GroupCorrelation(int sequence, OptionalInt groupSize) {
    this.sequence = sequence;
    this.groupSize = groupSize.orElse(-1);
  }

  /**
   * Gets the sequence or ordering number for this message in the the correlation group.
   *
   * @return the sequence number or null value if the sequence is not important
   */
  public int getSequence() {
    return sequence;
  }

  /**
   * Determines how many messages are in the correlation group
   *
   * @return total messages in this group or null value if the size is not known
   */
  public OptionalInt getGroupSize() {
    return groupSize > 0 ? OptionalInt.of(groupSize) : empty();
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(120);

    // format message for multi-line output, single-line is not readable
    buf.append("{");
    buf.append("sequence=").append(getSequence());
    buf.append("; groupSize=").append(getGroupSize().isPresent() ? getGroupSize().getAsInt() : NOT_SET);
    buf.append('}');
    return buf.toString();
  }
}

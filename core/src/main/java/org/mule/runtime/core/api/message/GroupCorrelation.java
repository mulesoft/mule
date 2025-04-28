/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.message;

import org.mule.runtime.api.message.ItemSequenceInfo;
import org.mule.runtime.api.message.Message;

import java.io.Serializable;
import java.util.OptionalInt;

/**
 * Immutable container for correlation properties relative to a {@link Message}.
 *
 * @since 4.0
 * @deprecated use {@link ItemSequenceInfo} instead
 */
public final class GroupCorrelation implements Serializable {


  public static final String NOT_SET = "<not set>";
  private static final long serialVersionUID = 8585292691365341650L;

  private ItemSequenceInfo wrappedInfo;

  public static GroupCorrelation of(int sequence) {
    return new GroupCorrelation(sequence);
  }

  public static GroupCorrelation of(int sequence, int groupSize) {
    return new GroupCorrelation(sequence, groupSize);
  }

  /**
   * Builds a new {@link GroupCorrelation} with the given parameters.
   *
   * @param sequence  see {@link #getSequence()}.
   * @param groupSize see {@link #getGroupSize()}.
   */
  private GroupCorrelation(int sequence, int groupSize) {
    this.wrappedInfo = ItemSequenceInfo.of(sequence, groupSize);
  }

  /**
   * Builds a new {@link GroupCorrelation} with the given parameters.
   *
   * @param sequence see {@link #getSequence()}.
   */
  private GroupCorrelation(int sequence) {
    this.wrappedInfo = ItemSequenceInfo.of(sequence);
  }

  /**
   * Gets the sequence or ordering number for this message in the the correlation group.
   *
   * @return the sequence number or null value if the sequence is not important
   */
  public int getSequence() {
    return wrappedInfo.getPosition();
  }

  /**
   * Determines how many messages are in the correlation group
   *
   * @return total messages in this group or null value if the size is not known
   */
  public OptionalInt getGroupSize() {
    return wrappedInfo.getSequenceSize();
  }

  @Override
  public String toString() {
    return wrappedInfo.toString();
  }
}

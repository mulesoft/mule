/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.message;

import static java.util.Optional.ofNullable;

import org.mule.runtime.api.message.MuleMessage;

import java.io.Serializable;
import java.util.Optional;

/**
 * Immutable container for correlation properties relative to a {@link MuleMessage}.
 * <p>
 * TODO MULE-10115 Review how correlation is implemented/achieved
 * 
 * @since 4.0
 */
public class Correlation implements Serializable {

  private static final long serialVersionUID = -5687080761804624442L;

  public static final String NOT_SET = "<not set>";

  private final String id;
  private final Integer groupSize;
  private final Integer sequence;

  /**
   * Builds a new {@link Correlation} with the given parameters.
   * 
   * @param id see {@link #getId()}.
   * @param groupSize see {@link #getGroupSize()}.
   * @param sequence see {@link #getSequence()}.
   */
  Correlation(String id, Integer groupSize, Integer sequence) {
    this.id = id;
    this.groupSize = groupSize;
    this.sequence = sequence;
  }

  /**
   * The correlation Id can be used by components in the system to manage message relations.
   * <p>
   * The id is associated with the message using the underlying transport protocol. As such not all messages will support the
   * notion of a id i.e. tcp or file. In this situation the correlation Id is set as a property of the message where it's up to
   * developer to keep the association with the message. For example if the message is serialised to xml the id will be available
   * in the message.
   *
   * @return the id for this message or null value if one hasn't been set
   */
  public Optional<String> getId() {
    return ofNullable(id);
  }

  /**
   * Gets the sequence or ordering number for this message in the the correlation group (as defined by {@link #getId()}).
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
    buf.append(" id=").append(getId().orElse(NOT_SET));
    buf.append("; groupSize=").append(getGroupSize().map(v -> v.toString()).orElse(NOT_SET));
    buf.append("; sequence=").append(getSequence().map(v -> v.toString()).orElse(NOT_SET));
    buf.append('}');
    return buf.toString();
  }
}

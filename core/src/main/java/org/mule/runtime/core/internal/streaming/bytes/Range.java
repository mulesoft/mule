/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.toIntExact;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.util.Preconditions.checkArgument;

import java.util.Optional;

/**
 * Represent a range in a zero based space of positions.
 *
 * Instances are immutable. All operations which alter state do it so by returning a new instance.
 *
 * @since 4.0
 */
public final class Range {

  private final long start;
  private final long end;

  /**
   * Creates a new instance
   *
   * @param start the range start position
   * @param end the range end position
   */
  protected Range(long start, long end) {
    checkArgument(end >= start, "end has to be greater than start");
    this.start = start;
    this.end = end;
  }

  /**
   * Moves the range by {@code offset} positions.
   * @param offset how many positions has the range been moved
   * @return a new advanced {@link Range}
   */
  protected Range advance(int offset) {
    return new Range(end, end + offset);
  }

  /**
   * @param range another {@link Range}
   * @return whether the given {@code range} is contained in {@code this} one
   */
  protected boolean contains(Range range) {
    return start <= range.start && end >= range.end;
  }

  /**
   * @param range another {@link Range}
   * @return whether {@code this} range is ahead of the given one. There might be overlap or not.
   */
  protected boolean isAhead(Range range) {
    return start > range.start && end >= range.end;
  }

  /**
   * @param range another {@link Range}
   * @return Whether {@code this} range ends before the given one.
   */
  protected boolean isBehind(Range range) {
    return end < range.end;
  }

  /**
   * @param range another {@link Range}
   * @return whether {@code this} range starts after the given {@code range} end
   */
  protected boolean startsAfter(Range range) {
    return start > range.end;
  }

  /**
   * If {@code this} range and the given one overlap, then it returns a
   * new range which represents the overlapping area.
   *
   * @param range another {@link Range}
   * @return a new {@link Range} with the overlapping area or {@link Optional#empty()} if there's no overlap
   */
  protected Optional<Range> overlap(Range range) {
    final long start = max(this.start, range.start);
    final long end = min(this.end, range.end);

    if (start <= end) {
      Range overlap = new Range(start, end);
      return contains(overlap) ? of(overlap) : empty();
    }

    return empty();
  }

  /**
   * @return the amount of positions comprised in {@code this} range
   */
  protected int length() {
    return toIntExact(end - start);
  }

  /**
   * @return whether this range start and end positions are equal.
   */
  protected boolean isEmpty() {
    return start == end;
  }

  public long getStart() {
    return start;
  }

  public long getEnd() {
    return end;
  }
}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.object;

/**
 * Represents the position of an item inside a {@link ObjectStreamBuffer}.
 * <p>
 * This object is immutable. All methods which generate new state will return
 * a new instance.
 *
 * @since 4.0
 */
public class Position implements Comparable<Position> {

  private final int bucketIndex;
  private final int itemIndex;

  public Position(int bucketIndex, int itemIndex) {
    this.bucketIndex = bucketIndex;
    this.itemIndex = itemIndex;
  }

  Position advanceItem() {
    return new Position(bucketIndex, itemIndex + 1);
  }

  Position advanceBucket() {
    return new Position(bucketIndex + 1, 0);
  }

  @Override
  public int compareTo(Position o) {
    int compare = bucketIndex - o.bucketIndex;
    if (compare == 0) {
      compare = itemIndex - o.itemIndex;
    }

    return compare;
  }

  public int getBucketIndex() {
    return bucketIndex;
  }

  public int getItemIndex() {
    return itemIndex;
  }
}

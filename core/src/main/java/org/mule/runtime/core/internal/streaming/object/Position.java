/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.object;

/**
 * Represents the position of an item inside a {@link ObjectStreamBuffer}. Because
 * the buffer stores its contents in buckets, this position will refer to bucket and
 * item indexes.
 * <p>
 * This object is immutable. All methods which generate new state will return
 * a new instance.
 *
 * @see Bucket
 * @since 4.0
 */
public class Position implements Comparable<Position> {

  private final int bucketIndex;
  private final int itemIndex;

  /**
   * Create a new instance
   * @param bucketIndex the index of the bucket which contains the item
   * @param itemIndex the item index between its bucket
   */
  public Position(int bucketIndex, int itemIndex) {
    this.bucketIndex = bucketIndex;
    this.itemIndex = itemIndex;
  }

  /**
   * @return a new {@link Position} with the same bucketIndex but a {@code +1} itemIndex
   */
  Position advanceItem() {
    return new Position(bucketIndex, itemIndex + 1);
  }

  /**
   * @return a new {@link Position} with the a {@code +1} bucketIndex and zero as the itemIndex
   */
  Position advanceBucket() {
    return new Position(bucketIndex + 1, 0);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int compareTo(Position o) {
    int compare = bucketIndex - o.bucketIndex;
    if (compare == 0) {
      compare = itemIndex - o.itemIndex;
    }

    return compare;
  }

  /**
   * @return {@code this} position's bucket index
   */
  public int getBucketIndex() {
    return bucketIndex;
  }

  /**
   * @return {@code this} position's item index
   */
  public int getItemIndex() {
    return itemIndex;
  }
}

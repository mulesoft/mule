/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.management.stats;

import org.mule.api.annotation.NoExtend;
import org.mule.runtime.api.component.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Contains statistics about the amount of data generated and consumed by a component.
 * <p>
 * Note: The {@code enabled} flag will be taken into account only at the point where a stream of data that may update this object
 * is generated. For instance, if a stream is generated while {@code enabled} flag is {@code false}, but when the stream is
 * consumed {@code enabled} flag is {@code true}, the consumption for that specific stream will not modify this object.
 *
 * @since 4.4, 4.3.1
 */
@NoExtend
public class DataFlowStatistics implements Statistics {

  private static final long serialVersionUID = 2335903369488757953L;

  protected boolean enabled = false;

  private final String componentLocation;
  private final String componentIdentifier;

  private final AtomicLong inputObjectCount = new AtomicLong();
  private final AtomicLong inputByteCount = new AtomicLong();
  private final AtomicLong outputObjectCount = new AtomicLong();
  private final AtomicLong outputByteCount = new AtomicLong();

  public DataFlowStatistics(String componentLocation, String componentIdentifier) {
    this.componentLocation = componentLocation;
    this.componentIdentifier = componentIdentifier;
  }

  /**
   * Ref: {@link Component#getLocation()}
   *
   * @return the location of the component this statistics are for. i.e: {@code flow/processors/2}
   */
  public String getComponentLocation() {
    return componentLocation;
  }

  /**
   * Ref: {@link Component#getIdentifier()}
   *
   * @return the id of the component this statistics are for. i.e: {@code http:request}
   */
  public String getComponentIdentifier() {
    return componentIdentifier;
  }

  /**
   * @return the amount of objects received by this component, if it receives objects.
   */
  public long getInputObjectCount() {
    return inputObjectCount.get();
  }

  /**
   * @return the amount of bytes received by this component, if it receives bytes.
   */
  public long getInputByteCount() {
    return inputByteCount.get();
  }

  /**
   * @return the amount of objects sent by this component, if it sends objects.
   */
  public long getOutputObjectCount() {
    return outputObjectCount.get();
  }

  /**
   * @return the amount of bytes sent by this component, if it sends bytes.
   */
  public long getOutputByteCount() {
    return outputByteCount.get();
  }

  public long addInputObjectCount(long delta) {
    return inputObjectCount.addAndGet(delta);
  }

  public long addInputByteCount(long delta) {
    return inputByteCount.addAndGet(delta);
  }

  public long addOutputObjectCount(long delta) {
    return outputObjectCount.addAndGet(delta);
  }

  public long addOutputByteCount(long delta) {
    return outputByteCount.addAndGet(delta);
  }

  /**
   * Returns true if this stats collector is enabled.
   * <p/>
   * This value does not affect statistics tabulation directly - it is up to the component to enable/disable collection based on
   * the value of this method.
   *
   * @return {@code true} if stats collection is enabled, otherwise false.
   */
  @Override
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Tags this stats collector as enabled or disabled.
   * <p/>
   * Does not affect stats calculation - it is up to the caller to check this flag.
   *
   * @param enabled {@code true} if stats should be enabled, otherwise false.
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

}

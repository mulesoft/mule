/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

import java.io.Serializable;
import java.util.Optional;

/**
 * Groups information about an item picked by a polling source and dispatched to the runtime for processing. It's main purpose is
 * to propagate the information until the {@link org.mule.runtime.core.api.event.CoreEvent} is created, so that a
 * {@link org.mule.runtime.api.notification.PollingSourceItemNotification} can be fired linking the item to the event.
 *
 * @since 4.5.0
 */
public class PollItemInformation {

  private final String pollId;
  private final String itemId;
  private final Optional<Serializable> watermark;
  private final String componentLocation;

  public PollItemInformation(String pollId, String itemId, Optional<Serializable> watermark, String componentLocation) {
    this.pollId = pollId;
    this.itemId = itemId;
    this.watermark = watermark;
    this.componentLocation = componentLocation;
  }

  public String getPollId() {
    return pollId;
  }

  public String getItemId() {
    return itemId;
  }

  public Optional<Serializable> getWatermark() {
    return watermark;
  }

  public String getComponentLocation() {
    return componentLocation;
  }
}

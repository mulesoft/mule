/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source.legacy;

import org.mule.runtime.extension.api.runtime.source.PollContext;

/**
 * Utility class for using legacy PollItemStatus.
 *
 * @since 4.4.0
 */
public class LegacyPollItemStatusUtils {

  /**
   * Gives the correspondent {@link PollContext.PollItemStatus} to the give {@link org.mule.sdk.api.runtime.source.PollContext.PollItemStatus}
   *
   * @param pollItemStatus  a {@link org.mule.sdk.api.runtime.source.PollContext.PollItemStatus}
   * @return                the correspondent {@link PollContext.PollItemStatus}
   */
  public static PollContext.PollItemStatus from(org.mule.sdk.api.runtime.source.PollContext.PollItemStatus pollItemStatus) {
    switch (pollItemStatus) {
      case SOURCE_STOPPING:
        return PollContext.PollItemStatus.SOURCE_STOPPING;
      case ALREADY_IN_PROCESS:
        return PollContext.PollItemStatus.ALREADY_IN_PROCESS;
      case FILTERED_BY_WATERMARK:
        return PollContext.PollItemStatus.FILTERED_BY_WATERMARK;
      case ACCEPTED:
        return PollContext.PollItemStatus.ACCEPTED;
      default:
        throw new IllegalArgumentException();
    }
  }

}

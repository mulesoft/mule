package org.mule.runtime.module.extension.internal.runtime.source.legacy;

import org.mule.runtime.extension.api.runtime.source.PollContext;

public class LegacyPollItemStatusUtils {

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

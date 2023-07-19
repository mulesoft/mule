/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.util.message;

import org.mule.runtime.api.message.ItemSequenceInfo;
import org.mule.runtime.core.api.message.GroupCorrelation;

public class ItemSequenceInfoUtils {


  public static ItemSequenceInfo fromGroupCorrelation(GroupCorrelation groupCorrelation) {
    ItemSequenceInfo itemSequenceInfo = null;
    if (groupCorrelation != null) {
      if (groupCorrelation.getGroupSize().isPresent()) {
        itemSequenceInfo = ItemSequenceInfo.of(groupCorrelation.getSequence(), groupCorrelation.getGroupSize().getAsInt());
      } else {
        itemSequenceInfo = ItemSequenceInfo.of(groupCorrelation.getSequence());
      }
    }
    return itemSequenceInfo;
  }

  public static GroupCorrelation toGroupCorrelation(ItemSequenceInfo itemSequenceInfo) {
    GroupCorrelation groupCorrelation = null;
    if (itemSequenceInfo != null) {
      if (itemSequenceInfo.getSequenceSize().isPresent()) {
        groupCorrelation = GroupCorrelation.of(itemSequenceInfo.getPosition(), itemSequenceInfo.getSequenceSize().getAsInt());
      } else {
        groupCorrelation = GroupCorrelation.of(itemSequenceInfo.getPosition());
      }
    }
    return groupCorrelation;
  }


}

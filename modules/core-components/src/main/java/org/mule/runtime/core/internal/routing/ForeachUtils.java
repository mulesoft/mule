/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.routing;

import static org.mule.runtime.core.api.util.StreamingUtils.updateTypedValueForStreaming;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.internal.routing.outbound.EventBuilderConfigurer;

class ForeachUtils {

  private ForeachUtils() {
    // nothing to do
  }

  static TypedValue manageTypedValueForStreaming(TypedValue typedValue, CoreEvent event, StreamingManager streamingManager) {
    if (typedValue.getValue() instanceof EventBuilderConfigurer) {
      return typedValue;
    } else if (typedValue.getValue() instanceof Message) {
      return updateTypedValueForStreaming(((Message) typedValue.getValue()).getPayload(),
                                          event, streamingManager);
    } else {
      return updateTypedValueForStreaming(typedValue, event, streamingManager);
    }
  }

}

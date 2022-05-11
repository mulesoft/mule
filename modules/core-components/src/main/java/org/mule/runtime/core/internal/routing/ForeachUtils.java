/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

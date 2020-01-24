/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static org.mule.runtime.api.exception.MuleException.INFO_LOCATION_KEY;
import static org.mule.runtime.api.exception.MuleException.INFO_SOURCE_XML_KEY;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.exception.MuleExceptionInfo;
import org.mule.runtime.api.notification.EnrichedNotificationInfo;
import org.mule.runtime.core.privileged.execution.LocationExecutionContextProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * Generates location info to augment MessagingExceptions with.
 */
public class MessagingExceptionLocationProvider extends LocationExecutionContextProvider {

  @Override
  public Map<String, Object> getContextInfo(EnrichedNotificationInfo notificationInfo, Component lastProcessed) {
    final Map<String, Object> info = new HashMap<>();
    info.put(INFO_LOCATION_KEY, lastProcessed.getRepresentation());

    final String source = lastProcessed.getDslSource();
    if (source != null) {
      info.put(INFO_SOURCE_XML_KEY, source);
    }
    return info;
  }

  @Override
  public void putContextInfo(MuleExceptionInfo info, EnrichedNotificationInfo notificationInfo, Component lastProcessed) {
    info.setLocation(lastProcessed.getRepresentation());

    final String source = lastProcessed.getDslSource();
    if (source != null) {
      info.setDslSource(source);
    }
  }
}

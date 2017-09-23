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
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.privileged.execution.LocationExecutionContextProvider;
import org.mule.runtime.api.notification.EnrichedNotificationInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * Generates location info to augment MessagingExceptions with.
 */
public class MessagingExceptionLocationProvider extends LocationExecutionContextProvider implements MuleContextAware {

  private MuleContext muleContext;

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  @Override
  public Map<String, Object> getContextInfo(EnrichedNotificationInfo notificationInfo, Component lastProcessed) {
    Map<String, Object> contextInfo = new HashMap<>();

    contextInfo.put(INFO_LOCATION_KEY, resolveProcessorRepresentation(muleContext.getConfiguration().getId(),
                                                                      lastProcessed.getLocation() != null
                                                                          ? lastProcessed.getLocation().getLocation()
                                                                          : null,
                                                                      lastProcessed));
    String sourceXML = getSourceXML(lastProcessed);
    if (sourceXML != null) {
      contextInfo.put(INFO_SOURCE_XML_KEY, sourceXML);
    }

    return contextInfo;
  }
}

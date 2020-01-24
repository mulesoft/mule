/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static java.util.Collections.singletonMap;
import static org.mule.runtime.api.exception.MuleException.INFO_LOCATION_KEY;
import static org.mule.runtime.api.exception.MuleException.INFO_SOURCE_XML_KEY;
import static org.mule.runtime.api.util.collection.SmallMap.of;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.notification.EnrichedNotificationInfo;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.privileged.execution.LocationExecutionContextProvider;

import java.util.Map;

/**
 * Generates location info to augment MessagingExceptions with.
 */
public class MessagingExceptionLocationProvider extends LocationExecutionContextProvider implements MuleContextAware {

  private String appId;

  @Override
  public void setMuleContext(MuleContext muleContext) {
    appId = muleContext.getConfiguration().getId();
  }

  @Override
  public Map<String, Object> getContextInfo(EnrichedNotificationInfo notificationInfo, Component lastProcessed) {
    final String processorRepresentation = resolveProcessorRepresentation(appId,
                                                                          lastProcessed.getLocation() != null
                                                                              ? lastProcessed.getLocation()
                                                                                  .getLocation()
                                                                              : null,
                                                                          lastProcessed);
    final String sourceXML = getSourceXML(lastProcessed);

    if (sourceXML != null) {
      return of(INFO_LOCATION_KEY, processorRepresentation,
                INFO_SOURCE_XML_KEY, sourceXML);
    } else {
      return singletonMap(INFO_LOCATION_KEY, processorRepresentation);
    }
  }
}

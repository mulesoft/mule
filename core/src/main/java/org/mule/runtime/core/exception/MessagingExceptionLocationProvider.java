/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.exception;

import static org.mule.runtime.core.api.LocatedMuleException.INFO_LOCATION_KEY;
import static org.mule.runtime.core.api.LocatedMuleException.INFO_SOURCE_XML_KEY;

import org.mule.runtime.api.meta.AnnotatedObject;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.construct.MessageProcessorPathResolver;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.execution.LocationExecutionContextProvider;
import org.mule.runtime.core.api.processor.MessageProcessor;

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
  public Map<String, Object> getContextInfo(MuleEvent event, MessageProcessor lastProcessed) {
    Map<String, Object> contextInfo = new HashMap<>();

    contextInfo.put(INFO_LOCATION_KEY, resolveProcessorRepresentation(muleContext.getConfiguration().getId(),
                                                                      getProcessorPath(event, lastProcessed), lastProcessed));
    if (lastProcessed instanceof AnnotatedObject) {
      String sourceXML = getSourceXML((AnnotatedObject) lastProcessed);
      if (sourceXML != null) {
        contextInfo.put(INFO_SOURCE_XML_KEY, sourceXML);
      }
    }

    return contextInfo;
  }

  protected String getProcessorPath(MuleEvent event, MessageProcessor lastProcessed) {
    if (event.getFlowConstruct() instanceof MessageProcessorPathResolver) {
      return ((MessageProcessorPathResolver) event.getFlowConstruct()).getProcessorPath(lastProcessed);
    } else {
      return lastProcessed.toString();
    }
  }
}

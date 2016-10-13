/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.endpoint.inbound;

import org.mule.compatibility.core.api.transport.Connector;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.config.ExceptionHelper;
import org.mule.runtime.core.util.ObjectUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sets error message properties as specified by the transport based on the exception type of the exception payload. This
 * mechanism uses a transport properties file in the META-INF/services/org/mule/config directory called
 * mule-exception-codes.properties. This property file maps the fully qualified class names of exceptions to the value of the
 * property that should be set. The name of the property is defined by the error.code.property property in the same properties
 * file.
 */
public class InboundExceptionDetailsMessageProcessor implements Processor, MuleContextAware {

  private static final Logger logger = LoggerFactory.getLogger(InboundExceptionDetailsMessageProcessor.class);

  protected Connector connector;
  private MuleContext muleContext;

  public InboundExceptionDetailsMessageProcessor(Connector connector) {
    this.connector = connector;
  }

  @Override
  public Event process(Event event) throws MuleException {
    if (event != null) {
      if (event.getMessage().getExceptionPayload() != null) {
        event = setExceptionDetails(event, connector, event.getMessage().getExceptionPayload().getException());
      }
    }
    return event;
  }

  /**
   * This method is used to set any additional and possibly transport specific information on the return message where it has an
   * exception payload.
   * 
   * @param event
   * @param exception
   */
  protected Event setExceptionDetails(Event event, Connector connector, Throwable exception) {
    String propName = ExceptionHelper.getErrorCodePropertyName(connector.getProtocol(), muleContext);
    // If we dont find a error code property we can assume there are not
    // error code mappings for this connector
    if (propName != null) {
      String code = ExceptionHelper.getErrorMapping(connector.getProtocol(), exception.getClass(), muleContext);
      if (logger.isDebugEnabled()) {
        logger.debug("Setting error code for: " + connector.getProtocol() + ", " + propName + "=" + code);
      }
      return Event.builder(event).message(InternalMessage.builder(event.getMessage()).addOutboundProperty(propName, code).build())
          .build();
    }
    return event;
  }

  @Override
  public String toString() {
    return ObjectUtils.toString(this);
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }
}

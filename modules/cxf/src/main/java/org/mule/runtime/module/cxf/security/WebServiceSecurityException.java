/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.cxf.security;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.config.i18n.MessageFactory;

public class WebServiceSecurityException extends org.mule.runtime.core.api.security.SecurityException {

  public WebServiceSecurityException(MuleEvent event, Throwable cause, MessageProcessor failingMessageProcessor) {
    super(MessageFactory
        .createStaticMessage("Security exception occurred invoking web service\nEndpoint = " + event.getMessageSourceURI()
            + "\nSecurity provider(s) = " + event.getMuleContext().getSecurityManager().getProviders() + "\nEvent = " + event),
          event, cause, failingMessageProcessor);
  }
}



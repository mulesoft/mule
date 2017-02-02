/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.cxf.support;

import static com.google.common.net.HttpHeaders.CONTENT_TYPE;

import org.mule.compatibility.module.cxf.CxfConstants;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;

import java.util.Collection;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

public class CopyAttachmentInInterceptor extends AbstractPhaseInterceptor {

  public CopyAttachmentInInterceptor() {
    super(Phase.PRE_INVOKE);
  }

  @Override
  public void handleMessage(Message message) throws Fault {
    Event event = (Event) message.getExchange().get(CxfConstants.MULE_EVENT);
    Collection<Attachment> atts = message.getAttachments();

    if (atts != null && !atts.isEmpty()) {
      message.getExchange().put(CxfConstants.MULE_EVENT, Event.builder(event).addVariable(CxfConstants.ATTACHMENTS, atts)
          .message(InternalMessage.builder(event.getMessage())
              .addOutboundProperty(CONTENT_TYPE, event.getMessage().getInboundProperty(CONTENT_TYPE)).build())
          .build());
    }
  }

}



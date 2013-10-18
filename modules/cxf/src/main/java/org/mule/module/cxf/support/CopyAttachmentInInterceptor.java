/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.support;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.module.cxf.CxfConstants;
import org.mule.transport.http.HttpConstants;

import java.util.Collection;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

public class CopyAttachmentInInterceptor extends AbstractPhaseInterceptor
{
    public CopyAttachmentInInterceptor()
    {
        super(Phase.PRE_INVOKE);
    }

    public void handleMessage(Message message) throws Fault
    {
        MuleEvent event = (MuleEvent) message.getExchange().get(CxfConstants.MULE_EVENT);
        MuleMessage muleMsg = event.getMessage();
        Collection<Attachment> atts = message.getAttachments();

        if (atts != null && !atts.isEmpty())
        {
            muleMsg.setInvocationProperty(CxfConstants.ATTACHMENTS, atts);
            muleMsg.setOutboundProperty(HttpConstants.HEADER_CONTENT_TYPE, muleMsg.getInboundProperty(HttpConstants.HEADER_CONTENT_TYPE));
        }
    }

}



/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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



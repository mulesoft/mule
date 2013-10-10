/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf.support;

import org.mule.api.MuleEvent;
import org.mule.module.cxf.CxfConstants;

import java.util.Collection;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

/**
 * Copies any attachments which were stored by the proxy to the outbound CXF message.
 */
public class CopyAttachmentOutInterceptor extends AbstractPhaseInterceptor
{
    public CopyAttachmentOutInterceptor()
    {
        super(Phase.SETUP);
    }

    public void handleMessage(Message message) throws Fault
    {
        MuleEvent event = (MuleEvent) message.getExchange().get(CxfConstants.MULE_EVENT);
        Collection<Attachment> a = event.getMessage().getInvocationProperty(CxfConstants.ATTACHMENTS);
        
        if (a != null) 
        {
            message.setAttachments(a);
        }
    }
}



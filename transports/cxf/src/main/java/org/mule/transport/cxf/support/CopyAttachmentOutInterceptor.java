/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf.support;

import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.transport.cxf.CxfConstants;

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
        MuleEvent event = RequestContext.getEvent();
        
        Collection<Attachment> a = (Collection<Attachment>) event.getMessage().getProperty(CxfConstants.ATTACHMENTS);
        if (a != null) 
        {
            message.setAttachments(a);
            event.getMessage().removeProperty(CxfConstants.ATTACHMENTS);
        }
    }
}



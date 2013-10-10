/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf.employee;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

public class AttachmentVerifyInterceptor extends AbstractPhaseInterceptor<Message>
{
    public static boolean HasAttachments;
    
    public AttachmentVerifyInterceptor()
    {
        super(Phase.PRE_STREAM_ENDING);
    }

    public void handleMessage(Message message) throws Fault
    {
        HasAttachments = message.getAttachments() != null && message.getAttachments().size() > 0;
    }
    

}



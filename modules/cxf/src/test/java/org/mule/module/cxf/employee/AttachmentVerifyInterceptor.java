/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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



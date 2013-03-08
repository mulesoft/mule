/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.jca;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChainBuilder;
import org.mule.module.jca.i18n.JcaMessages;
import org.mule.service.AbstractService;

/**
 * <code>JcaService</code> Is the type of service used in Mule when embedded inside
 * an app server using JCA. In the future we might want to use one of the existing
 * models.
 */
@Deprecated
public class JcaService extends AbstractService
{

    /**
     * Serial version
     */
    private static final long serialVersionUID = -1510441245219710451L;

    
    public JcaService(MuleContext muleContext)
    {
        super(muleContext);
    }
    
    /**
     * This is the synchronous call method and not supported by components managed in
     * a JCA container
     * 
     * @param event
     * @throws MuleException
     */
    public MuleEvent sendEvent(MuleEvent event) throws MuleException
    {
        throw new UnsupportedOperationException("sendEvent()");
    }

    public boolean isPaused()
    {
        // JcaService is a wrapper for a hosted service implementation and
        // therefore cannot be paused by mule
        return false;
    }

    protected void waitIfPaused(MuleEvent event) throws InterruptedException
    {
        // JcaService is a wrapper for a hosted service implementation and
        // therefore cannot be paused by mule
    }

    protected void doPause() 
    {
        throw new UnsupportedOperationException(JcaMessages.cannotPauseResumeJcaComponent().getMessage());
    }

    protected void doResume() 
    {
        throw new UnsupportedOperationException(JcaMessages.cannotPauseResumeJcaComponent().getMessage());
    }

    @Override
    protected void addMessageProcessors(MessageProcessorChainBuilder builder)
    {
        builder.chain(new MessageProcessor()
        {
            // Wrap to prevent lifecycle propagation. Component is given lifecycle
            // directly by AbstractService
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                return component.process(event);
            }
        });
    }
}

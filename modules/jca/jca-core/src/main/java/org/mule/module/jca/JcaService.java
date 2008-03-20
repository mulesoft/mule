/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.jca;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.service.ServiceException;
import org.mule.module.jca.i18n.JcaMessages;
import org.mule.service.AbstractService;

/**
 * <code>JcaService</code> Is the type of service used in Mule when embedded inside
 * an app server using JCA. In the future we might want to use one of the existing
 * models.
 */
public class JcaService extends AbstractService
{

    /**
     * Serial version
     */
    private static final long serialVersionUID = -1510441245219710451L;

    /**
     * This is the synchronous call method and not supported by components managed in
     * a JCA container
     * 
     * @param event
     * @return
     * @throws MuleException
     */
    public MuleMessage sendEvent(MuleEvent event) throws MuleException
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

    protected void doPause() throws MuleException
    {
        throw new ServiceException(JcaMessages.cannotPauseResumeJcaComponent(), null, this);
    }

    protected void doResume() throws MuleException
    {
        throw new ServiceException(JcaMessages.cannotPauseResumeJcaComponent(), null, this);
    }

    protected void doDispatch(MuleEvent event) throws MuleException
    {
        component.onCall(event);
    }

    /**
     * Implementation of template method which is never call because send() is
     * overwritten
     */
    protected MuleMessage doSend(MuleEvent event) throws MuleException
    {
        return null;
    }

}

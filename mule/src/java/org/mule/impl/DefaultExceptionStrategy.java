/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk 
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */
package org.mule.impl;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArrayList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.impl.message.ExceptionMessage;
import org.mule.impl.message.ExceptionPayload;
import org.mule.transformers.xml.ObjectToXml;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.RecoverableException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.config.ExceptionHelper;

import java.util.Iterator;
import java.util.List;

/**
 * <code>DefaultExceptionStrategy</code> Provides a default exception handling strategy.  The class final thus to
 * change exception handling behaviour the user must reimplemented the ExceptionListener Interface
 *
 * @author Ross Mason
 * @version $Revision$
 */

public class DefaultExceptionStrategy extends AbstractExceptionListener implements Initialisable
{
    public void handleMessagingException(UMOMessage message, Throwable t) {
        defaultHandler(t);
        routeException(message, null, t);
    }

    public void handleRoutingException(UMOMessage message, UMOEndpoint endpoint, Throwable t) {
        defaultHandler(t);
        routeException(message, endpoint, t);
    }

    public void handleLifecycleException(Object component, Throwable t)
    {
        defaultHandler(t);
        logger.error("The object that failed was: \n" + component.toString());
    }

    protected void defaultHandler(Throwable t) {
        logException(t);
        RequestContext.setExceptionPayload(new ExceptionPayload(t));
    }

    public void handleStandardException(Throwable t)
    {
        defaultHandler(t);
    }

    protected void logException(Throwable t) {
        UMOException umoe = ExceptionHelper.getRootMuleException(t);
        if(umoe!=null) {
            logger.error(umoe.getDetailedMessage());
        } else {
            logger.error("Caught exception in Exception Strategy: " + t.getMessage(), t);
        }
    }

    protected void logFatal(UMOMessage message, Throwable t) {
        logger.fatal("Failed to dispatch message to error queue after it failed to process.  This may cause message loss." +
                (message==null ? "" : "Logging Message here: \n" + message.toString()), t);
    }

    protected void routeException(UMOMessage message, UMOEndpoint failedEndpoint, Throwable t)
    {
        UMOEndpoint endpoint = getEndpoint(t);
        if(endpoint!=null) {
            try {
                logger.error("Message being processed is: " + (message==null ? "null" : message.toString()));
                UMOEventContext ctx = RequestContext.getEventContext();
                ExceptionMessage msg = null;
                if(failedEndpoint!=null) {
                    msg = new ExceptionMessage(message, endpoint, t, ctx);
                } else {
                    msg = new ExceptionMessage(message, t, ctx);
                }
                ctx.dispatchEvent(msg, endpoint);
                logger.debug("routed Exception message via " + endpoint);
            } catch (UMOException e) {
                logFatal(message, e);
            }
        }
    }

   protected UMOEndpoint getEndpoint(Throwable t) {
       if(endpoints.size() > 0) {
           return (UMOEndpoint)endpoints.get(0);
       } else {
           return null;
       }
   }

    public void initialise() throws InitialisationException, RecoverableException
    {
        for (Iterator iterator = endpoints.iterator(); iterator.hasNext();) {
            UMOEndpoint umoEndpoint = (UMOEndpoint) iterator.next();
            umoEndpoint.initialise();
        }
    }
}

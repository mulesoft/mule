/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.samples.errorhandler;

import org.mule.MuleServer;
import org.mule.samples.errorhandler.handlers.DefaultHandler;
import org.mule.samples.errorhandler.handlers.FatalHandler;
import org.mule.umo.UMOException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>ErrorManager</code> TODO (document class)
 */
public class ErrorManager
{
    /** logger used by this class */
    private static final Log logger = LogFactory.getLog(ErrorManager.class);

    private Map handlers = new HashMap();
    private ExceptionHandler defaultHandler = null;

    public ErrorManager()
    {
        defaultHandler = new DefaultHandler();
    }

    public void setHandlers(List handlers)
    {
        Iterator handlerIter = handlers.iterator();
        while (handlerIter.hasNext())
        {
            ExceptionHandler handler = (ExceptionHandler)handlerIter.next();
            this.addHandler(handler);
        }
    }

    public void addHandler(ExceptionHandler eh)
    {
        for (Iterator i = eh.getRegisteredClasses(); i.hasNext();)
        {
            handlers.put(i.next(), eh);
        }
    }

    public ExceptionHandler getHandler(Class exceptionClass)
    {
        Object obj = handlers.get(exceptionClass);
        if (obj == null)
        {
            obj = handlers.get(Throwable.class);
        }

        return (ExceptionHandler)obj;
    }

    public void onException(ErrorMessage msg) throws UMOException
    {
        Class eClass = null;
        ExceptionHandler eh = null;

        try
        {
            eClass = msg.getException().toException().getClass();
            eh = getHandler(eClass);
            eh.onException(msg);
        }
        catch (Exception e)
        {

            logger.error(LocaleMessage.handlerFailure(eh));

            if (eh instanceof DefaultHandler)
            {
                logger.error(LocaleMessage.defaultFatalHandling(FatalHandler.class));
                handleFatal(e);

            }
            else if (eh instanceof FatalHandler)
            {
                logger.fatal(LocaleMessage.fatalHandling(e));
                MuleServer.getManagementContext().dispose();
                System.exit(-1);
            }
            else
            {
                logger.error(LocaleMessage.defaultHandling(DefaultHandler.class, eh, e));
                handleDefault(msg, e);
            }
        }
    }

    private void handleDefault(ErrorMessage msg, Throwable t)
    {
        ErrorMessage nestedMsg = null;
        // Try wrapping the exception and the Exception message that caused the
        // exception in a new message
        try
        {
            nestedMsg = new ErrorMessage(t);
        }
        catch (Exception e)
        {
            logger.fatal(LocaleMessage.defaultException(e), e);
            handleFatal(e);
        }
        try
        {
            defaultHandler.onException(nestedMsg);
        }
        catch (HandlerException e)
        {
            logger.fatal(LocaleMessage.defaultHandlerException(e), e);
            handleFatal(e);
        }

    }

    private void handleFatal(Throwable t)
    {
        // If this method has been called, all other handlers failed
        // this is all we can do
        logger.fatal(LocaleMessage.fatalException(t), t);
        MuleServer.getManagementContext().dispose();
        System.exit(-1);
    }
}

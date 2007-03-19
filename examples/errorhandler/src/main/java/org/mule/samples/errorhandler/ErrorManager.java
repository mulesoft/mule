/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.samples.errorhandler;

import org.mule.samples.errorhandler.handlers.DefaultHandler;
import org.mule.samples.errorhandler.handlers.FatalHandler;
import org.mule.umo.UMOException;

import org.mule.config.i18n.Message;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>ErrorManager</code> TODO (document class)
 * 
 */
public class ErrorManager
{
    /** logger used by this class */
    private static transient Log logger = LogFactory.getLog(ErrorManager.class);

    private Map handlers = new HashMap();
    private ExceptionHandler defaultHandler = null;

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.impl.MuleUMO#initialise(java.util.Properties)
     */
    public ErrorManager()
    {
        defaultHandler = new DefaultHandler();
    }

    public void setHandlers(ExceptionHandler[] eh)
    {
        for (int i = 0; i < eh.length; i++)
        {
            addHandler(eh[i]);
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
        
            logger.error(new Message("errorhandler-example", 6, 
               (eh != null ? (eh.getClass().getName() + " : " + e) : "null")).getMessage());

            if (eh instanceof DefaultHandler)
            {
                logger.error(new Message("errorhandler-example", 7, 
                    FatalHandler.class.getName()).getMessage());
                handleFatal(e);

            }
            else if (eh instanceof FatalHandler)
            {
                logger.fatal(new Message("errorhandler-example", 8, 
                    e).getMessage());
                //TODO Fix this
                //managementContext.shutdown(e, false);
            }
            else
            {
                logger.error(new Message("errorhandler-example", 9, 
                    DefaultHandler.class.getName(),
                    (eh != null ? (eh.getClass().getName() + " : " + e) : "null")).getMessage());
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
            logger.fatal(new Message("errorhandler-example", 10, e).getMessage(), e);
            handleFatal(e);
        }
        try
        {
            defaultHandler.onException(nestedMsg);
        }
        catch (HandlerException e)
        {
            logger.fatal(new Message("errorhandler-example", 11, e).getMessage(), e);
            handleFatal(e);
        }

    }

    private void handleFatal(Throwable t)
    {
        // If this method has been called, all other handlers failed
        // this is all we can do
        logger.fatal(new Message("errorhandler-example", 12, t).getMessage(), t);
        //TODO fix this
        //managementContext.shutdown(t, false);
    }
}

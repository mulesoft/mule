/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.errorhandler;

import org.mule.api.MuleException;
import org.mule.example.errorhandler.handlers.DefaultHandler;
import org.mule.example.errorhandler.handlers.FatalHandler;

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

    public void onException(ErrorMessage msg) throws MuleException
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
            logger.error(e);

            if (eh instanceof DefaultHandler)
            {
                logger.error(LocaleMessage.defaultFatalHandling(FatalHandler.class));
                handleFatal(e);

            }
            else if (eh instanceof FatalHandler)
            {
                logger.fatal(LocaleMessage.fatalHandling(e));
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
    }
}

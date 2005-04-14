/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */

package org.mule.samples.errorhandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleManager;
import org.mule.impl.RequestContext;
import org.mule.samples.errorhandler.handlers.DefaultHandler;
import org.mule.samples.errorhandler.handlers.FatalHandler;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 
 * <code>ErrorManager</code> TODO (document class)
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ErrorManager
{
    /** logger used by this class */
    private static transient Log logger = LogFactory.getLog(ErrorManager.class);

    private Map handlers = new HashMap();
    private ExceptionHandler defaultHandler = null;
    private ExceptionHandler fatalHandler = null;
    private UMOEvent currentEvent;

    /*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.impl.MuleUMO#initialise(java.util.Properties)
	 */
    public ErrorManager()
    {
        defaultHandler = new DefaultHandler();
        fatalHandler = new FatalHandler();
    }
    
    public void setHandlers(ExceptionHandler[] eh)
    {
        for(int i = 0; i < eh.length; i ++)
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

        return (ExceptionHandler) obj;
    }
    
    public void onException(ErrorMessage msg) throws UMOException
    {
        Class eClass = null;
        ExceptionHandler eb = null;
        // the other way to obtain a reference to the current event is to implement
        //the org.mule.umo.lifecycle.Callable or AsynchonousCallable
        currentEvent = RequestContext.getEvent();
        try
        {
            eClass = msg.getException().toException().getClass();
            eb = getHandler(eClass);
            eb.onException(msg);
        }
        catch (Exception e)
        {

            logger.error("Failed to handle Exception using handler: " + eb.getClass().getName() + " : " + e);
            if (eb instanceof DefaultHandler)
            {
                logger.error(
                    "As the failure happened in the Default Exception handler, now using Fatal Behaviour "
                        + FatalHandler.class.getName()
                        + " which will cause the Exception Manager to shutdown");

                handleFatal(e);

            }
            else if (eb instanceof FatalHandler)
            {
                logger.fatal("Exception caught handling Fatal exception: " + e);
                ((MuleManager)MuleManager.getInstance()).shutdown(e, false);
            }
            else
            {
                logger.error(
                    "Exception Handler resorting to Default Behaviour : "
                        + DefaultHandler.class.getName()
                        + ", due to exception in configured behavour : "
                        + eb.getClass().getName()
                        + " : "
                        + e);
                handleDefault(msg, e);

            }
        }
    }

    private void handleDefault(ErrorMessage msg, Throwable t)
    {
        ErrorMessage nestedMsg = null;
        //Try wrapping the exception and the Exception message that caused the
        //exception in a new message
        try
        {
            nestedMsg = new ErrorMessage(t);
        }
        catch (Exception e)
        {
            logger.fatal("Exception happened while handling and exception using the Default behaviour: " + e, e);
            handleFatal(e);
        }
        try
        {
            defaultHandler.onException(nestedMsg);
        }
        catch (HandlerException e)
        {
            logger.fatal("Exception happened while handling and exception using the Default behaviour: " + e, e);
            handleFatal(e);
        }

    }

    private void handleFatal(Throwable t)
    {
        //If this method has been called, all other handlers failed
        //this is all we can do
        logger.fatal("An exception has been caught be the Fatal Exception Behaviour");
        logger.fatal("Exception is: " + t, t);
        ((MuleManager)MuleManager.getInstance()).shutdown(t, false);
    }
}

/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.testmodels.mule;

import org.mule.api.MuleEvent;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.exception.RollbackSourceCallback;
import org.mule.api.exception.SystemExceptionHandler;
import org.mule.exception.AbstractExceptionListener;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>TestExceptionStrategy</code> is used by the Mule test cases as a direct
 * replacement of the {@link org.mule.exception.AbstractMessagingExceptionStrategy}.
 * This is used to test that overriding the default Exception strategy works.
 */
public class TestExceptionStrategy extends AbstractExceptionListener implements MessagingExceptionHandler, SystemExceptionHandler
{
    /**
     * logger used by this class
     */
    protected final Log logger = LogFactory.getLog(getClass());

    /**
     * This is the lock that protect both the storage of {@link #callback} and
     * modifications of {@link #unhandled}.
     */
    private Object callbackLock = new Object();

    // @GuardedBy("callbackLock")
    private ExceptionCallback callback;
    // @GuardedBy("callbackLock")
    private List<Exception> unhandled = new LinkedList<Exception>();

    private volatile String testProperty;

    public TestExceptionStrategy()
    {
    }
    
    public String getTestProperty()
    {
        return testProperty;
    }

    public void setTestProperty(String testProperty)
    {
        this.testProperty = testProperty;
    }

    public MuleEvent handleException(Exception exception, MuleEvent event, RollbackSourceCallback rollbackMethod)
    {
        ExceptionCallback callback = null;
        synchronized (callbackLock)
        {
            if (this.callback != null)
            {
                callback = this.callback;
            }
            else
            {
                unhandled.add(exception);
            }
        }
        // It is important that the call to the callback is done outside
        // synchronization since we don't control that code and
        // we could have liveness problems.
        logger.info("Handling exception: " + exception.getClass().getName());
        if (callback != null)
        {
            logger.info("Exception caught on TestExceptionStrategy and was sent to callback.", exception);
            callback.onException(exception);
        }
        else
        {
            logger.info("Exception caught on TestExceptionStrategy but there was no callback set.", exception);
        }
        return event;
    }

    public MuleEvent handleException(Exception exception, MuleEvent event)
    {
        return handleException(exception, event, null);
    }

    public void handleException(Exception exception, RollbackSourceCallback rollbackMethod)
    {
        handleException(exception, null, rollbackMethod);
    }

    public void handleException(Exception exception)
    {
        handleException(exception, null, null);
    }

    public interface ExceptionCallback
    {
        void onException(Throwable t);
    }

    public void setExceptionCallback(ExceptionCallback exceptionCallback)
    {
        synchronized (callbackLock)
        {
            this.callback = exceptionCallback;
        }
        processUnhandled();
    }

    protected void processUnhandled()
    {
        List<Exception> unhandledCopies = null;
        ExceptionCallback callback = null;
        synchronized (callbackLock)
        {
            if (this.callback != null)
            {
                callback = this.callback;
                unhandledCopies = new ArrayList<Exception>(unhandled);
                unhandled.clear();
            }
        }
        // It is important that the call to the callback is done outside
        // synchronization since we don't control that code and
        // we could have liveness problems.
        if (callback != null && unhandledCopies != null)
        {
            for (Exception exception : unhandledCopies)
            {
                logger.info("Handling exception after setting the callback.", exception);
                callback.onException(exception);
            }
        }
    }
}

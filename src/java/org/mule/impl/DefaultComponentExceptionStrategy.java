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
 */
package org.mule.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.management.stats.ComponentStatistics;
import org.mule.transaction.TransactionCoordination;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOTransaction;

/**
 * <code>DefaultComponentExceptionStrategy</code> is the default exception
 * handler for compoennts.
 *
 * The handler logs errors and will forward the message and exception to an exception
 * endpointUri if one is set on this Exception strategy
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class DefaultComponentExceptionStrategy extends DefaultExceptionStrategy
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(DefaultComponentExceptionStrategy.class);

    /**
     * The component to which the Exception handler belongs
     */
    private UMOComponent component;

    private ComponentStatistics statistics;

    public DefaultComponentExceptionStrategy()
    {

    }
    /**
     * Constructor
     *
     * @param component the owner of this exception strategy
     * @see DefaultLifecycleAdapter
     */
    public DefaultComponentExceptionStrategy(UMOComponent component)
    {
        super();
        setComponent(component);
    }

     /**
     * @return the UniversalMessageObject to which this handler is attached
     */
    public UMOComponent getComponent()
    {
        return component;
    }

    /**
     * This is called when an exception occurs. By implementing this you can provide different stratgies
     * for handling exceptions
     *
     * @param message Can be anthing, but is usually The message being processed when
     *                the exception occurred.  The message could be an event and implmenting methods
     *                should expect that an UMOEvent maybe passed to this method from the
     *                framework.
     * @param t       The Throwable exception that occurred
     * @return java.lang.Throwable The user may wish to return any exception which could be thrown on
     *         depending on implementation
     */
    public Throwable handleException(Object message, Throwable t)
    {
        if(component==null) {
            UMOEvent event = RequestContext.getEvent();
            if(event==null) { //very bad unlikely to happen
                logger.fatal("The Default Component Exception Strategy has been invoked but there is no current event on the context");
                logger.fatal("The error is: " + t.getMessage(), t);
            } else {
                setComponent(event.getComponent());
            }
        }

        if(statistics!=null) {
            statistics.incExecutionError();
        }

        if(component!=null) {
            logger.error("Caught exception in Exception Strategy for: " + component.getDescriptor().getName() + ": " + t, t);
        }

        UMOTransaction tx = TransactionCoordination.getInstance().getTransaction();
        if(tx!=null) {
            handleTransaction(tx);
        }

        try
        {
            routeException(message, t);
        } catch (UMOException e)
        {
            logger.fatal("Failed to route Exception message, this may result in unexpected message loss");
            if(statistics!=null) {
                statistics.incFatalError();
            }
        }
        return null;
    }

    protected void routeException(Object message, Throwable t) throws UMOException
    {
        if(getEndpoint()!=null) {
            super.routeException(message, t);
                statistics.getOutboundRouterStat().incrementRoutedMessage(getEndpoint());
            }
        }


    public void setComponent(UMOComponent component)
    {
        this.component = component;
        if(component instanceof MuleComponent) {
            this.statistics = ((MuleComponent)component).getStatistics();
        }
    }
}

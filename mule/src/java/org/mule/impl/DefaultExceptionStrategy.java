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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.impl.message.ExceptionMessage;
import org.mule.transaction.TransactionCoordination;
import org.mule.transformers.xml.ObjectToXml;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOException;
import org.mule.umo.UMOExceptionStrategy;
import org.mule.umo.UMOTransaction;
import org.mule.umo.UMOTransactionException;
import org.mule.umo.endpoint.UMOEndpoint;

/**
 * <code>DefaultExceptionStrategy</code> Provides a default exception handling strategy.  The class final thus to
 * change exception handling behaviour the user must reimplemented the ExceptionListener Interface
 *
 * @author Ross Mason
 * @version $Revision$
 */

public class DefaultExceptionStrategy implements UMOExceptionStrategy
{
      /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(DefaultComponentExceptionStrategy.class);

    private UMOEndpoint exceptionEndpoint;

    public DefaultExceptionStrategy()
    {

    }

    public UMOEndpoint getEndpoint()
    {
        return exceptionEndpoint;
    }

    public void setEndpoint(UMOEndpoint exceptionEndpoint)
    {
        this.exceptionEndpoint = exceptionEndpoint;
        if(exceptionEndpoint!=null) {
            if(exceptionEndpoint.getTransformer()==null) {
                exceptionEndpoint.setTransformer(new ObjectToXml());
            }
            exceptionEndpoint.setType(UMOEndpoint.ENDPOINT_TYPE_SENDER);
        }
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
     */
     public void handleException(Object message, Throwable t)
    {
        logger.error("Caught exception in Exception Strategy: " + t.getMessage(), t);

        UMOTransaction tx = TransactionCoordination.getInstance().getTransaction();
        if(tx!=null) {
            handleTransaction(tx);
        } else {
            logger.debug("There is no current transaction to roll back after exception");
        }

        try
        {
            routeException(message, t);
        } catch (UMOException e)
        {
            logger.fatal("Failed to route Exception message, this may result in unexpected message loss. Endpoint is: "
                    + (exceptionEndpoint==null ? null : exceptionEndpoint.getEndpointURI() ));
        }
    }

    protected void routeException(Object message, Throwable t) throws UMOException
    {
        if(exceptionEndpoint!=null) {
            logger.error("Message being processed is: " + (message==null ? "null" : message.toString()));
            UMOEventContext ctx = RequestContext.getEventContext();
            ExceptionMessage msg = new ExceptionMessage(message, t, ctx);
            ctx.dispatchEvent(msg, exceptionEndpoint);
            logger.debug("routed Exception message via " + exceptionEndpoint);
        }
    }

    protected void handleTransaction(UMOTransaction tx)
    {
        logger.warn("Marking current transaction for rollback: " + tx);
        try {
        	tx.setRollbackOnly();
        } catch (UMOTransactionException e) {
        	logger.error("Could not mark transaction for rollback", e);
        }
    }
}

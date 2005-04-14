/*
 * $Header$ $Revision$ $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved. http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 *  
 */

package org.mule.providers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleRuntimeException;
import org.mule.config.MuleProperties;
import org.mule.config.ThreadingProfile;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.RequestContext;
import org.mule.transaction.TransactionCoordination;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOTransaction;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.manager.UMOWorkManager;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageDispatcher;

import javax.resource.spi.work.Work;
import java.beans.ExceptionListener;

/**
 * <p/>
 * <code>AbstractMessageDispatcher</code> TODO (document class)
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public abstract class AbstractMessageDispatcher implements UMOMessageDispatcher, ExceptionListener {
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    /**
     * Thread pool of Connector sessions
     */
    protected UMOWorkManager workManager = null;

    protected boolean disposeOnCompletion = false;

    protected AbstractConnector connector;

    protected boolean disposed = false;

    protected boolean doThreading = true;

    public AbstractMessageDispatcher(AbstractConnector connector) {
        init(connector);
        disposeOnCompletion = ((AbstractConnector) connector).isDisposeDispatcherOnCompletion();
    }

    private void init(AbstractConnector connector) {
        this.connector = connector;
        if (connector instanceof AbstractConnector) {
            ThreadingProfile profile = ((AbstractConnector) connector).getDispatcherThreadingProfile();
            doThreading = profile.isDoThreading();
            if (doThreading) {
                workManager = profile.createWorkManager(connector.getName());
                try {
                    workManager.start();
                } catch (UMOException e) {
                    throw new MuleRuntimeException(new Message(Messages.FAILED_TO_START_X, "WorkManager"), e);
                }
            }
        }
    }

    /*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.umo.provider.UMOMessageDispatcher#dispatch(org.mule.umo.UMOEvent)
	 */
    public final void dispatch(UMOEvent event) throws Exception {
        try {
            event.setSynchronous(false);
            event.setProperty(MuleProperties.MULE_ENDPOINT_PROPERTY, event.getEndpoint().getEndpointURI().toString());
            RequestContext.setEvent(event);
            //Apply Security filter if one is set
            UMOEndpoint endpoint = event.getEndpoint();
            if (endpoint.getSecurityFilter() != null) {
                try {
                    endpoint.getSecurityFilter().authenticate(event);
                } catch (org.mule.umo.security.SecurityException e) {
                    logger.warn("Outbound Request was made but was not authenticated: " + e.getMessage(), e);
                    connector.handleException(e);
                    return;
                }
            }
            //the security filter may update the payload so we need to get the
            //latest event again
            event = RequestContext.getEvent();

            UMOTransaction tx = TransactionCoordination.getInstance().getTransaction();
            if (doThreading && !event.isSynchronous() && tx == null) {
                workManager.scheduleWork(new Worker(event));
            } else {
                doDispatch(event);
            }
        } catch (Exception e) {
            //automatically dispose if there were failures
            logger.info("Exception occurred while executing on this dispatcher. disposing before continuing");
            dispose();
            throw e;
        } finally{
            if (disposeOnCompletion) {
                dispose();
            }
        }
    }


    public final UMOMessage send(UMOEvent event) throws Exception {
        try {
            event.setSynchronous(true);
            event.setProperty(MuleProperties.MULE_ENDPOINT_PROPERTY, event.getEndpoint().getEndpointURI().toString());
            RequestContext.setEvent(event);
            //Apply Security filter if one is set
            UMOEndpoint endpoint = event.getEndpoint();
            if (endpoint.getSecurityFilter() != null) {
                try {
                    endpoint.getSecurityFilter().authenticate(event);
                } catch (org.mule.umo.security.SecurityException e) {
                    logger.warn("Outbound Request was made but was not authenticated: " + e.getMessage(), e);
                    connector.handleException(e);
                    return event.getMessage();
                }
            }
            //the security filter may update the payload so we need to get the
            //latest event again
            event = RequestContext.getEvent();
            try {
                UMOMessage result = doSend(event);

                return result;
            } catch (Exception e) {
                //automatically dispose if there were failures
                logger.info("Exception occurred while executing on this dispatcher. disposing before continuing");
                dispose();
                throw e;
            }
        } finally {
            if (disposeOnCompletion) {
                dispose();
            }
        }
    }


    /*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.util.ExceptionListener#onException(java.lang.Throwable)
	 */
    public void exceptionThrown(Exception e) {
        getConnector().handleException(e);

    }

    public boolean isDisposed() {
        return disposed;
    }

    /**
     * Template method to destroy any resources.  some connector will want to cache
     * dispatchers and destroy them themselves
     */
    public final void dispose() {
        if (!disposed) {
            try {
                doDispose();
            } finally {
                connector.getDispatchers().values().remove(this);
                disposed = true;
            }
        }

    }

    public UMOConnector getConnector() {
        return connector;
    }

    public abstract void doDispose();

    public abstract void doDispatch(UMOEvent event) throws Exception;

    public abstract UMOMessage doSend(UMOEvent event) throws Exception;

    private class Worker implements Work {
        private UMOEvent event;

        public Worker(UMOEvent event) {
            this.event = event;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Runnable#run()
         */
        public void run() {
            try {
                RequestContext.setEvent(event);
                doDispatch(event);

            } catch (Exception e) {
                dispose();
                getConnector().handleException(e);
            } finally {
                if (disposeOnCompletion) {
                    dispose();
                }
            }
        }

        public void release() {
        }
    }
}

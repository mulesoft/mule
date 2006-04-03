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
package org.mule.impl.model;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleManager;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.DefaultComponentExceptionStrategy;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.RequestContext;
import org.mule.impl.internal.notifications.ComponentNotification;
import org.mule.management.stats.ComponentStatistics;
import org.mule.umo.ComponentException;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.UMOManager;
import org.mule.umo.model.UMOModel;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.provider.UMOMessageDispatcher;
import org.mule.util.ClassHelper;
import org.mule.util.concurrent.WaitableBoolean;

import java.beans.ExceptionListener;

/**
 * todo document
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public abstract class AbstractComponent implements UMOComponent {
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    /**
     * The Mule descriptor associated with the component
     */
    protected MuleDescriptor descriptor = null;

    protected ComponentStatistics stats = null;

    /**
     * Determines if the component has been stopped
     */
    protected AtomicBoolean stopped = new AtomicBoolean(true);

    /**
     * Determines whether stop has been called and is still in progress
     */
    protected WaitableBoolean stopping = new WaitableBoolean(false);

    /**
     * Determines if the component has been paused
     */
    protected WaitableBoolean paused = new WaitableBoolean(false);

    /**
     * determines if the proxy pool has been initialised
     */
    protected AtomicBoolean poolInitialised = new AtomicBoolean(false);

    /**
     * The exception strategy used by the component, this is provided by the
     * UMODescriptor
     */
    protected ExceptionListener exceptionListener = null;

    /**
     * Determines if the component has been initilised
     */
    protected AtomicBoolean initialised = new AtomicBoolean(false);

    protected UMOModel model;

    /**
     * Default constructor
     */
    public AbstractComponent(MuleDescriptor descriptor, UMOModel model) {
        if (descriptor == null) {
            throw new IllegalArgumentException("Descriptor cannot be null");
        }
        this.descriptor = descriptor;
        this.model = MuleManager.getInstance().getModel();
    }

    /**
     * Initialise the component. The component will first create a Mule UMO from
     * the UMODescriptor and then initialise a pool based on the attributes in
     * the UMODescriptor.
     *
     * @throws org.mule.umo.lifecycle.InitialisationException if the component fails to initialise
     * @see org.mule.umo.UMODescriptor
     */
    public final synchronized void initialise() throws InitialisationException {
        if (initialised.get()) {
            throw new InitialisationException(new Message(Messages.OBJECT_X_ALREADY_INITIALISED, "Component '"
                    + descriptor.getName() + "'"), this);
        }
        descriptor.initialise();

        this.exceptionListener = descriptor.getExceptionListener();

        // initialise statistics
        stats = new ComponentStatistics(getName(),
                descriptor.getPoolingProfile().getMaxActive(),
                descriptor.getThreadingProfile().getMaxThreadsActive());

        stats.setEnabled(((MuleManager) MuleManager.getInstance()).getStatistics().isEnabled());
        ((MuleManager) MuleManager.getInstance()).getStatistics().add(stats);
        stats.setOutboundRouterStat(getDescriptor().getOutboundRouter().getStatistics());
        stats.setInboundRouterStat(getDescriptor().getInboundRouter().getStatistics());

        doInitialise();
        initialised.set(true);
        fireComponentNotification(ComponentNotification.COMPONENT_INITIALISED);

    }

    protected void fireComponentNotification(int action) {
        MuleManager.getInstance().fireNotification(new ComponentNotification(descriptor, action));
    }

    void finaliseEvent(UMOEvent event) {
        logger.debug("Finalising event for: " + descriptor.getName() + " event endpointUri is: "
                + event.getEndpoint().getEndpointURI());
        // queue.remove(event);
    }

    public void forceStop() throws UMOException {
        if (!stopped.get()) {
            logger.debug("Stopping UMOComponent");
            stopping.set(true);
            fireComponentNotification(ComponentNotification.COMPONENT_STOPPING);
            doForceStop();
            stopped.set(true);
            stopping.set(false);
            fireComponentNotification(ComponentNotification.COMPONENT_STOPPED);
        }
    }

    public void stop() throws UMOException {
        if (!stopped.get()) {
            logger.debug("Stopping UMOComponent");
            stopping.set(true);
            fireComponentNotification(ComponentNotification.COMPONENT_STOPPING);
            if (MuleManager.getInstance().getQueueManager().getQueueSession().getQueue(descriptor.getName() + ".component").size() > 0) {
                try {
                    stopping.whenFalse(null);
                } catch (InterruptedException e) {
                    //we can ignore this
                }
            }

            doStop();
            stopped.set(true);
            fireComponentNotification(ComponentNotification.COMPONENT_STOPPED);
        }
    }

    public void start() throws UMOException {
        if (stopped.get()) {
            stopped.set(false);
            doStart();
        }
        fireComponentNotification(ComponentNotification.COMPONENT_STARTED);
    }

    public final void pause() {
        doPause();
        paused.set(true);
        fireComponentNotification(ComponentNotification.COMPONENT_PAUSED);
    }

    public final void resume() {
        doResume();
        paused.set(false);
        fireComponentNotification(ComponentNotification.COMPONENT_RESUMED);
    }

    public final void dispose() {
        try {
            if (!stopped.get()) {
                stop();
            }
        } catch (UMOException e) {
            logger.error("Failed to stop component: " + descriptor.getName(), e);
        }
        doDispose();
        fireComponentNotification(ComponentNotification.COMPONENT_DISPOSED);
        ((MuleManager) MuleManager.getInstance()).getStatistics().remove(stats);
    }

    public ComponentStatistics getStatistics() {
        return stats;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMOSession#getDescriptor()
     */
    public UMODescriptor getDescriptor() {
        return descriptor;
    }

    public void dispatchEvent(UMOEvent event) throws UMOException {
        if (stopping.get() || stopped.get()) {
            throw new ComponentException(new Message(Messages.COMPONENT_X_IS_STOPPED, getDescriptor().getName()), event.getMessage(), this);
        }
        // Dispatching event to an inbound endpoint
        // in the MuleSession#dispatchEvent
        if (!event.getEndpoint().canReceive()) {
            UMOMessageDispatcher dispatcher = event.getEndpoint().getConnector().getDispatcher(event.getEndpoint());
            try {
                dispatcher.dispatch(event);
            } catch (Exception e) {
                throw new DispatchException(event.getMessage(), event.getEndpoint(), e);
            }
            return;
        }

        // Dispatching event to the component
        if (stats.isEnabled()) {
            stats.incReceivedEventASync();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Component: " + descriptor.getName() + " has received asynchronous event on: "
                    + event.getEndpoint().getEndpointURI());
        }

        doDispatch(event);
    }

    public UMOMessage sendEvent(UMOEvent event) throws UMOException {
        if (stopping.get() || stopped.get()) {
            throw new ComponentException(new Message(Messages.COMPONENT_X_IS_STOPPED, getDescriptor().getName()), event.getMessage(), this);
        }
        if (logger.isDebugEnabled() && paused.get()) {
            logger.debug("Component: " + descriptor.getName() + " is paused. Blocking call until resume is called");
        }
        try {
            paused.whenFalse(null);
        } catch (InterruptedException e) {
            throw new ComponentException(event.getMessage(), this, e);
        }

        if (stats.isEnabled()) {
            stats.incReceivedEventSync();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Component: " + descriptor.getName() + " has received synchronous event on: "
                    + event.getEndpoint().getEndpointURI());
        }
        RequestContext.setEvent(event);
        UMOMessage result = doSend(event);
        return result;
    }

    /**
     * @return the Mule descriptor name which is associated with the component
     */
    public String getName() {
        return descriptor.getName();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return descriptor.getName();
    }

    public boolean isStopped() {
        return stopped.get();
    }

    public boolean isStopping() {
        return stopping.get();
    }

    public boolean isPaused() {
        return paused.get();
    }

    protected void handleException(Exception e) {
        RequestContext.getEvent().getEndpoint().getConnector().getExceptionListener().exceptionThrown(e);
        if (exceptionListener instanceof DefaultComponentExceptionStrategy) {
            if (((DefaultComponentExceptionStrategy) exceptionListener).getComponent() == null) {
                ((DefaultComponentExceptionStrategy) exceptionListener).setComponent(this);
            }
        }
        exceptionListener.exceptionThrown(e);
    }

    /**
     * Provides a consistent mechanism for custom models to create components.
     * @return
     * @throws UMOException
     */
    protected Object createComponent() throws UMOException
    {
        UMOManager manager = MuleManager.getInstance();
        Object impl = descriptor.getImplementation();
        Object component = null;

        if (impl instanceof String) {
            String reference = impl.toString();

            if (reference.startsWith(MuleDescriptor.IMPLEMENTATION_TYPE_LOCAL)) {
                String refName = reference.substring(MuleDescriptor.IMPLEMENTATION_TYPE_LOCAL.length());
                component = descriptor.getProperties().get(refName);
                if (component == null) {
                    throw new InitialisationException(new Message(Messages.NO_LOCAL_IMPL_X_SET_ON_DESCRIPTOR_X,
                                                                  refName,
                                                                  descriptor.getName()), this);
                }
            }

            if (component == null) {
                if (descriptor.isContainerManaged()) {
                    component = manager.getContainerContext().getComponent(reference);
                } else {
                    try {
                        component = ClassHelper.instanciateClass(reference, new Object[] {});
                    } catch (Exception e) {
                        throw new InitialisationException(new Message(Messages.CANT_INSTANCIATE_NON_CONTAINER_REF_X,
                                                                      reference), e, descriptor);
                    }
                }
            }
            if(descriptor.isSingleton()) {
                descriptor.setImplementation(component);
            }
        } else {
            component = impl;
        }

        try {
            BeanUtils.populate(component, descriptor.getProperties());
        } catch (Exception e) {
            throw new InitialisationException(new Message(Messages.FAILED_TO_SET_PROPERTIES_ON_X, "Component '"
                    + descriptor.getName() + "'"), e, descriptor);
        }
        // Call any custom initialisers
        descriptor.fireInitialisationCallbacks(component);

        return component;
    }

    protected void doForceStop() throws UMOException {

    }

    protected void doStop() throws UMOException {

    }
    protected void doStart()  throws UMOException {

    }

    protected void doPause() {

    }

    protected void doResume() {

    }

    protected void doDispose() {

    }

    protected void doInitialise() throws InitialisationException {

    }

    public boolean isStarted() {
        return !stopped.get();
    }

    protected abstract UMOMessage doSend(UMOEvent event) throws UMOException;

    protected abstract void doDispatch(UMOEvent event) throws UMOException;
}

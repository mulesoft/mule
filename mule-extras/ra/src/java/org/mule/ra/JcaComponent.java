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
 */
package org.mule.ra;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleException;
import org.mule.MuleManager;
import org.mule.config.MuleProperties;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.RequestContext;
import org.mule.impl.internal.notifications.ComponentNotification;
import org.mule.management.stats.ComponentStatistics;
import org.mule.umo.*;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.RecoverableException;
import org.mule.umo.model.ModelException;
import org.mule.umo.model.UMOEntryPoint;
import org.mule.util.ClassHelper;

import java.lang.reflect.Method;

/**
 * <code>JcaComponent</code> Is the type of component used in mul when embedded inside
 * an app server using JCA.
 * If future we might want to use one of the existing models
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class JcaComponent implements UMOComponent
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(JcaComponent.class);

    private MuleDescriptor descriptor;
    private UMOEntryPoint entryPoint;
    private Object component;

    private boolean started = false;

    /**
     * Determines if the component has been initilised
     */
    private AtomicBoolean initialised = new AtomicBoolean(false);

    private ComponentStatistics stats = null;

    public JcaComponent(MuleDescriptor descriptor)
    {
        if (descriptor == null) {
            throw new IllegalArgumentException("Descriptor cannot be null");
        }
        this.descriptor = descriptor;
    }

    public UMODescriptor getDescriptor()
    {
        return descriptor;
    }

    public void dispatchEvent(UMOEvent event) throws UMOException
    {
        Object result = null;
        try {
            // Invoke method
            result = entryPoint.invoke(component, RequestContext.getEventContext());

        } catch (Exception e) {
            throw new MuleException(new Message(Messages.FAILED_TO_INVOKE_X, "UMO Component: " + descriptor.getName()),
                                    e);
        }
    }

    /**
     * This is the synchronous call method and not supported by components
     * managed in a JCA container
     * 
     * @param event
     * @return
     * @throws UMOException
     */
    public UMOMessage sendEvent(UMOEvent event) throws UMOException
    {
        throw new UnsupportedOperationException("sendEvent()");
    }

    public void pause() throws UMOException
    {
    }

    public void resume() throws UMOException
    {
    }

    public void start() throws UMOException
    {
        started = true;
    }

    public void stop() throws UMOException
    {
        started = false;
    }

    public void dispose()
    {
        ((MuleManager) MuleManager.getInstance()).getStatistics().remove(stats);
    }

    public synchronized void initialise() throws InitialisationException, RecoverableException
    {
        if (initialised.get()) {
            throw new InitialisationException(new Message(Messages.OBJECT_X_ALREADY_INITIALISED, "Component '"
                    + descriptor.getName() + "'"), this);
        }
        descriptor.initialise();
        try {
            entryPoint = MuleManager.getInstance().getModel().getEntryPointResolver().resolveEntryPoint(descriptor);
        } catch (ModelException e) {
            throw new InitialisationException(e, this);
        }

        // initialise statistics
        stats = new ComponentStatistics(descriptor.getName(), -1, -1);

        stats.setEnabled(((MuleManager) MuleManager.getInstance()).getStatistics().isEnabled());
        ((MuleManager) MuleManager.getInstance()).getStatistics().add(stats);
        stats.setOutboundRouterStat(getDescriptor().getOutboundRouter().getStatistics());
        stats.setInboundRouterStat(getDescriptor().getInboundRouter().getStatistics());

        component = descriptor.getImplementation();

        initialised.set(true);
        MuleManager.getInstance().fireNotification(new ComponentNotification(descriptor, ComponentNotification.COMPONENT_INITIALISED));
    }

    protected Object getDelegateComponent() throws InitialisationException
    {
        Object impl = descriptor.getImplementation();
        Object component = null;

        String reference = impl.toString();
        if (reference.startsWith(MuleDescriptor.IMPLEMENTATION_TYPE_LOCAL)) {
            String refName = reference.substring(MuleDescriptor.IMPLEMENTATION_TYPE_LOCAL.length());
            component = descriptor.getProperties().get(refName);
            if (component == null) {
                throw new InitialisationException(new Message(Messages.NO_LOCAL_IMPL_X_SET_ON_DESCRIPTOR_X,
                                                              refName,
                                                              descriptor.getName()), this);
            }
        } else {
            throw new InitialisationException(new Message(Messages.NO_LOCAL_IMPL_X_SET_ON_DESCRIPTOR_X,
                                                          MuleDescriptor.IMPLEMENTATION_TYPE_LOCAL + "xxx",
                                                          descriptor.getName()), this);
        }

        // Call any custom initialisers
        descriptor.fireInitialisationCallbacks(component);
        return component;
    }

    public boolean isStarted() {
        return started; 
    }
}

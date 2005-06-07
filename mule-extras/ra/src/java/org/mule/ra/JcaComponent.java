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

import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleException;
import org.mule.MuleManager;
import org.mule.config.MuleProperties;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.RequestContext;
import org.mule.impl.internal.events.ComponentEvent;
import org.mule.management.stats.ComponentStatistics;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.RecoverableException;
import org.mule.umo.model.ModelException;
import org.mule.umo.model.UMOEntryPoint;
import org.mule.util.ClassHelper;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;

/**
 * <code>JcaComponent</code> TODO
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

    /**
     * Determines if the component has been initilised
     */
    private SynchronizedBoolean initialised = new SynchronizedBoolean(false);

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
            // Check for method override
            Object methodOverride = event.removeProperty(MuleProperties.MULE_METHOD_PROPERTY);
            Method method = null;
            if (methodOverride instanceof Method) {
                method = (Method) methodOverride;
            } else if (methodOverride != null) {
                method = ClassHelper.getMethod(methodOverride.toString(), component.getClass());
            }
            // Invoke method
            result = entryPoint.invoke(component, RequestContext.getEventContext(), method);

            // UMOMessage resultMessage = null;
            // if(result==null && entryPoint.isVoid()) {
            // resultMessage = new MuleMessage(event.getTransformedMessage(),
            // event.getProperties());
            // } else if(result!=null) {
            // if(result instanceof UMOMessage) {
            // resultMessage = (UMOMessage)result;
            // } else {
            // resultMessage = new MuleMessage(result, event.getProperties());
            // }
            // }
            // boolean stopProcessing = false;
            // if(descriptor.getResponseRouter()!=null) {
            // stopProcessing =
            // descriptor.getResponseRouter().isStopProcessing();
            // } else {
            // stopProcessing = event.isStopFurtherProcessing();
            // }

            // Need to find a cleaner solution for handling response messages
            // Right now routing is split between here a nd the proxy
            // if(descriptor.getResponseRouter()!=null) {
            // if(event.isSynchronous() && !stopProcessing) {
            // //we need to do the outbound first but we dispatch aynshonously
            // as
            // //we are waiting for a response on another resource
            // stopProcessing = true;
            // descriptor.getOutboundRouter().route(resultMessage,
            // event.getSession(), false);
            // }
            // logger.debug("Waiting for response router message");
            // result =
            // descriptor.getResponseRouter().getResponse(resultMessage);
            //
            // if (stopProcessing) {
            // logger.debug("Setting stop oubound processing according to
            // response router");
            // RequestContext.getEvent().setStopFurtherProcessing(true);
            // }
            // return result;
            // } else {
            // return resultMessage;
            // }
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
    }

    public void stop() throws UMOException
    {
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
        MuleManager.getInstance().fireEvent(new ComponentEvent(descriptor, ComponentEvent.COMPONENT_INITIALISED));
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
}

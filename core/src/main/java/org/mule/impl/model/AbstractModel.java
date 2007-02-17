/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.model;

import org.mule.MuleManager;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.DefaultComponentExceptionStrategy;
import org.mule.impl.DefaultLifecycleAdapterFactory;
import org.mule.impl.ImmutableMuleDescriptor;
import org.mule.impl.MuleSession;
import org.mule.impl.internal.notifications.ModelNotification;
import org.mule.impl.model.resolvers.DynamicEntryPointResolver;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOException;
import org.mule.umo.UMOSession;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.LifecycleNotifications;
import org.mule.umo.lifecycle.UMOLifecycleAdapterFactory;
import org.mule.umo.manager.UMOServerNotification;
import org.mule.umo.model.ModelException;
import org.mule.umo.model.UMOEntryPointResolver;
import org.mule.umo.model.UMOModel;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentSkipListMap;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;

import java.beans.ExceptionListener;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>MuleModel</code> is the default implementation of the UMOModel. The model
 * encapsulates and manages the runtime behaviour of a Mule Server instance. It is
 * responsible for maintaining the UMOs instances and their configuration.
 */
public abstract class AbstractModel implements UMOModel, LifecycleNotifications 
// TODO the LifecycleNotifications should be fired using AOP
{
    public static final String DEFAULT_MODEL_NAME = "main";
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    private String name = DEFAULT_MODEL_NAME;
    private UMOEntryPointResolver entryPointResolver= new DynamicEntryPointResolver();
    private UMOLifecycleAdapterFactory lifecycleAdapterFactory = new DefaultLifecycleAdapterFactory();

    private Map components = new ConcurrentSkipListMap();

    /**
     * Collection for mule descriptors registered in this Manager
     */
    protected Map descriptors = new ConcurrentHashMap();

    private AtomicBoolean initialised = new AtomicBoolean(false);

    private AtomicBoolean started = new AtomicBoolean(false);

    private ExceptionListener exceptionListener = new DefaultComponentExceptionStrategy();

    /**
     * Default constructor
     */
    public AbstractModel()
    {
        // Always set default entrypoint resolver, lifecycle and compoenent
        // resolver and exceptionstrategy.
        entryPointResolver = new DynamicEntryPointResolver();
        lifecycleAdapterFactory = new DefaultLifecycleAdapterFactory();
        components = new ConcurrentSkipListMap();
        descriptors = new ConcurrentHashMap();
        exceptionListener = new DefaultComponentExceptionStrategy();
    }

    public String getId()
    {
        return getClass().getName() + "." + getName();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMOModel#getName()
     */
    public String getName()
    {
        return name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMOModel#setName(java.lang.String)
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.model.UMOModel#getEntryPointResolver()
     */
    public UMOEntryPointResolver getEntryPointResolver()
    {
        return entryPointResolver;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.model.UMOModel#setEntryPointResolver(org.mule.umo.model.UMOEntryPointResolver)
     */
    public void setEntryPointResolver(UMOEntryPointResolver entryPointResolver)
    {
        this.entryPointResolver = entryPointResolver;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMOModel#isUMORegistered(java.lang.String)
     */
    public boolean isComponentRegistered(String name)
    {
        return (components.get(name) != null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.model.UMOModel#getLifecycleAdapterFactory()
     */
    public UMOLifecycleAdapterFactory getLifecycleAdapterFactory()
    {
        return lifecycleAdapterFactory;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.model.UMOModel#setLifecycleAdapterFactory(org.mule.umo.lifecycle.UMOLifecycleAdapterFactory)
     */
    public void setLifecycleAdapterFactory(UMOLifecycleAdapterFactory lifecycleAdapterFactory)
    {
        this.lifecycleAdapterFactory = lifecycleAdapterFactory;
    }

    /**
     * Destroys any current components
     */
    public void dispose()
    {
        MuleManager.getInstance().fireNotification(getNotificationDisposing());

        for (Iterator i = components.values().iterator(); i.hasNext();)
        {
            UMOComponent component = (UMOComponent)i.next();
            try
            {
                component.dispose();
                logger.info(component + " has been destroyed successfully");
            }
            catch (Exception e1)
            {
                logger.warn("Failed to dispose component: " + e1.getMessage());
            }
        }

        components.clear();
        descriptors.clear();

        MuleManager.getInstance().fireNotification(getNotificationDisposed());
    }

    /**
     * Returns a valid component for the given Mule name
     * 
     * @param muleName the Name of the Mule for which the component is required
     * @return a component for the specified name
     */
    public UMOSession getComponentSession(String muleName)
    {
        UMOComponent component = (UMOComponent)components.get(muleName);
        if (component == null)
        {
            logger.warn("Component: " + muleName + " not found returning null session");
            return null;
        }
        else
        {
            return new MuleSession(component);
        }
    }

    /**
     * Stops any registered components
     * 
     * @throws UMOException if a Component fails tcomponent
     */
    public void stop() throws UMOException
    {
        MuleManager.getInstance().fireNotification(getNotificationStopping());
        for (Iterator i = components.values().iterator(); i.hasNext();)
        {
            UMOComponent component = (UMOComponent)i.next();
            component.stop();
            logger.info("Component " + component + " has been stopped successfully");
        }
        MuleManager.getInstance().fireNotification(getNotificationStopped());
    }

    /**
     * Starts all registered components
     * 
     * @throws UMOException if any of the components fail to start
     */
    public void start() throws UMOException
    {
        if (!initialised.get())
        {
            initialise();
        }

        if (!started.get())
        {
            MuleManager.getInstance().fireNotification(getNotificationStarting());

            for (Iterator i = components.values().iterator(); i.hasNext();)
            {
                AbstractComponent component = (AbstractComponent)i.next();

                if (component.getDescriptor().getInitialState().equals(
                    ImmutableMuleDescriptor.INITIAL_STATE_STARTED))
                {
                    component.start();
                    logger.info("Component " + component + " has been started successfully");
                }
                else if (component.getDescriptor().getInitialState().equals(
                    ImmutableMuleDescriptor.INITIAL_STATE_PAUSED))
                {
                    component.start(true);
                    logger.info("Component " + component
                                + " has been started and paused (initial state = 'paused')");
                }
                else
                {
                    logger.info("Component " + component
                                + " has not been started (initial state = 'stopped')");
                }
            }
            started.set(true);
            MuleManager.getInstance().fireNotification(getNotificationStarted());
        }
        else
        {
            logger.debug("Model already started");
        }
    }

    /**
     * Starts a single Mule Component. This can be useful when stopping and starting
     * some Mule UMOs while letting others continue
     * 
     * @param name the name of the Mule UMO to start
     * @throws UMOException if the MuleUMO is not registered or the component failed
     *             to start
     */
    public void startComponent(String name) throws UMOException
    {
        UMOComponent component = (UMOComponent)components.get(name);
        if (component == null)
        {
            throw new ModelException(new Message(Messages.COMPONENT_X_NOT_REGISTERED, name));
        }
        else
        {
            component.start();
            logger.info("Mule " + component.toString() + " has been started successfully");
        }
    }

    /**
     * Stops a single Mule Component. This can be useful when stopping and starting
     * some Mule UMOs while letting others continue.
     * 
     * @param name the name of the Mule UMO to stop
     * @throws UMOException if the MuleUMO is not registered
     */
    public void stopComponent(String name) throws UMOException
    {
        UMOComponent component = (UMOComponent)components.get(name);
        if (component == null)
        {
            throw new ModelException(new Message(Messages.COMPONENT_X_NOT_REGISTERED, name));
        }
        else
        {
            component.stop();
            logger.info("Mule " + name + " has been stopped successfully");
        }
    }

    /**
     * Pauses event processing for a single Mule Component. Unlike stopComponent(), a
     * paused component will still consume messages from the underlying transport,
     * but those messages will be queued until the component is resumed. <p/> In
     * order to persist these queued messages you can set the 'recoverableMode'
     * property on the Muleconfiguration to true. this causes all internal queues to
     * store their state.
     * 
     * @param name the name of the Mule UMO to stop
     * @throws org.mule.umo.UMOException if the MuleUMO is not registered or the
     *             component failed to pause.
     * @see org.mule.config.MuleConfiguration
     */
    public void pauseComponent(String name) throws UMOException
    {
        UMOComponent component = (UMOComponent)components.get(name);

        if (component != null)
        {
            component.pause();
            logger.info("Mule Component " + name + " has been paused successfully");
        }
        else
        {
            throw new ModelException(new Message(Messages.COMPONENT_X_NOT_REGISTERED, name));
        }
    }

    /**
     * Resumes a single Mule Component that has been paused. If the component is not
     * paused nothing is executed.
     * 
     * @param name the name of the Mule UMO to resume
     * @throws org.mule.umo.UMOException if the MuleUMO is not registered or the
     *             component failed to resume
     */
    public void resumeComponent(String name) throws UMOException
    {
        UMOComponent component = (UMOComponent)components.get(name);

        if (component != null)
        {
            component.resume();
            logger.info("Mule Component " + name + " has been resumed successfully");
        }
        else
        {
            throw new ModelException(new Message(Messages.COMPONENT_X_NOT_REGISTERED, name));
        }
    }

//    public void setServiceDescriptors(List descriptors) throws UMOException
//    {
//        for (Iterator iterator = descriptors.iterator(); iterator.hasNext();)
//        {
//            registerComponent((UMODescriptor)iterator.next());
//        }
//    }

    public void initialise() throws InitialisationException
    {
        if (!initialised.get())
        {
            MuleManager.getInstance().fireNotification(getNotificationInitialising());

            if (exceptionListener instanceof Initialisable)
            {
                ((Initialisable)exceptionListener).initialise();
            }
            UMOComponent component = null;
            for (Iterator i = components.values().iterator(); i.hasNext();)
            {
                component = (UMOComponent)i.next();

                component.initialise();

                logger.info("Component " + component.getDescriptor().getName()
                            + " has been started successfully");
            }
            initialised.set(true);
            MuleManager.getInstance().fireNotification(getNotificationInitialised());
        }
        else
        {
            logger.debug("Model already initialised");
        }
    }

    public ExceptionListener getExceptionListener()
    {
        return exceptionListener;
    }

    public void setExceptionListener(ExceptionListener exceptionListener)
    {
        this.exceptionListener = exceptionListener;
    }

    public UMODescriptor getDescriptor(String name)
    {
        return (UMODescriptor)descriptors.get(name);
    }

    public UMOComponent getComponent(String name)
    {
        return (UMOComponent)components.get(name);
    }

    public void addComponent(UMODescriptor descriptor, UMOComponent component)
    {
        descriptors.put(descriptor.getName(), descriptor);
        components.put(descriptor.getName(), component);
    }

    public UMOComponent removeComponent(String name)
    {
        descriptors.remove(name);
        return (UMOComponent) components.remove(name);
    }
    
    /**
     * Gets an iterator of all component names registered in the model
     * 
     * @return an iterator of all component names
     */
    public Iterator getComponentNames()
    {
        return components.keySet().iterator();
    }

    public UMOServerNotification getNotificationInitialised()
    {
        return new ModelNotification(this, ModelNotification.MODEL_INITIALISED);
    }

    public UMOServerNotification getNotificationInitialising()
    {
        return new ModelNotification(this, ModelNotification.MODEL_INITIALISING);
    }

    public UMOServerNotification getNotificationDisposed()
    {
        return new ModelNotification(this, ModelNotification.MODEL_DISPOSED);
    }

    public UMOServerNotification getNotificationDisposing()
    {
        return new ModelNotification(this, ModelNotification.MODEL_DISPOSING);
    }

    public UMOServerNotification getNotificationStarted()
    {
        return new ModelNotification(this, ModelNotification.MODEL_STARTED);
    }

    public UMOServerNotification getNotificationStarting()
    {
        return new ModelNotification(this, ModelNotification.MODEL_STARTING);
    }

    public UMOServerNotification getNotificationStopped()
    {
        return new ModelNotification(this, ModelNotification.MODEL_STOPPED);
    }

    public UMOServerNotification getNotificationStopping()
    {
        return new ModelNotification(this, ModelNotification.MODEL_STOPPING);
    }

    public UMOServerNotification getNotificationPaused()
    {
        return null;
    }

    public UMOServerNotification getNotificationResumed()
    {
        return null;
    }
}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.component;

import org.mule.DefaultMuleEventContext;
import org.mule.VoidResult;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.component.JavaComponent;
import org.mule.api.component.LifecycleAdapter;
import org.mule.api.lifecycle.Callable;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.object.ObjectFactory;
import org.mule.api.registry.ServiceException;
import org.mule.config.i18n.CoreMessages;
import org.mule.object.SingletonObjectFactory;
import org.mule.transformer.TransformerTemplate;

import java.util.Collections;

/**
 * Simple {@link JavaComponent} implementation to be used when
 * {@link LifecycleAdapter} is not required because i) the object instance implements
 * {@link Callable} and so entry-point resolution is required and ii) component bindings
 * are not used.<br/> An {@link ObjectFactory} can be set but must return object
 * instances that implement {@link Callable}. If one of the constructors that takes
 * just a Class or the instance itself is used then the
 * {@link SingletonObjectFactory} is used by default. <br/> This implementation
 * replaces and improves on <code>OptimizedComponent</code>/<code>OptimizedMuleProxy</code>
 */
public class SimpleCallableJavaComponent extends AbstractJavaComponent
{

    private boolean started = false;

    public SimpleCallableJavaComponent()
    {
        // for spring
    }

    /**
     * Create an SimpleCallableJavaComponent instance using an object instance that
     * implements {@link Callable}
     * 
     * @param callable
     */
    public SimpleCallableJavaComponent(Callable callable)
    {
        objectFactory = new SingletonObjectFactory(callable);
    }

    /**
     * Create an SimpleCallableJavaComponent instance using an object class. This
     * class should implement {@link Callable}.
     * 
     * @param callable
     * @throws DefaultMuleException if the Class specified does not implement
     *             {@link Callable}
     */
    public SimpleCallableJavaComponent(Class callable) throws DefaultMuleException
    {
        if (!(Callable.class.isAssignableFrom(callable)))
        {
            throw new DefaultMuleException(CoreMessages.objectNotOfCorrectType(callable, Callable.class));
        }
        objectFactory = new SingletonObjectFactory(callable);
    }

    public SimpleCallableJavaComponent(ObjectFactory objectFactory) throws DefaultMuleException
    {
        if (!(Callable.class.isAssignableFrom(objectFactory.getObjectClass())))
        {
            throw new DefaultMuleException(CoreMessages.objectNotOfCorrectType(objectFactory.getObjectClass(),
                Callable.class));
        }
        this.objectFactory = objectFactory;
    }

    @Override
    protected void doStart() throws MuleException
    {
        super.doStart();
        if (Startable.class.isAssignableFrom(objectFactory.getObjectClass()))
        {
            try
            {
                ((Startable) objectFactory.getInstance(muleContext)).start();
            }
            catch (Exception e)
            {
                throw new ServiceException(CoreMessages.failedToStart("Service '" + flowConstruct.getName() + "'"), e);
            }
        }
    }

    @Override
    protected void doStop() throws MuleException
    {
        super.doStop();
        if (started && Stoppable.class.isAssignableFrom(objectFactory.getObjectClass()))
        {
            try
            {
                ((Stoppable) objectFactory.getInstance(muleContext)).stop();
            }
            catch (Exception e)
            {
                throw new ServiceException(CoreMessages.failedToStop("Service '" + flowConstruct.getName() + "'"), e);
            }
        }
    }

    @Override
    protected void doDispose()
    {
        super.doDispose();
        if (Disposable.class.isAssignableFrom(objectFactory.getObjectClass()))
        {
            try
            {
                ((Disposable) objectFactory.getInstance(muleContext)).dispose();
            }
            catch (Exception e)
            {
                logger.error("Unable to dispose component instance", e);
            }
        }
    }

    @Override
    public Class getObjectType()
    {
        if (objectFactory != null)
        {
            return objectFactory.getObjectClass();
        }
        else
        {
            return Callable.class;
        }
    }

    @Override
    protected LifecycleAdapter borrowComponentLifecycleAdaptor() throws Exception
    {
        // no-op
        return null;
    }

    @Override
    protected void returnComponentLifecycleAdaptor(LifecycleAdapter lifecycleAdapter)
    {
        // no-op
    }

    @Override
    protected Object invokeComponentInstance(MuleEvent event) throws Exception
    {
        Object result = ((Callable) objectFactory.getInstance(muleContext)).onCall(new DefaultMuleEventContext(event));
        if (result instanceof VoidResult)
        {
            // This will rewire the current message
            return event.getMessage();
        }
        else if (result != null)
        {
            if (result instanceof MuleMessage)
            {
                return result;
            }
            else
            {
                event.getMessage().applyTransformers(
                    event, Collections.singletonList(new TransformerTemplate(new TransformerTemplate.OverwitePayloadCallback(
                        result))));
                return event.getMessage();
            }
        }
        else
        {
            return null;
        }
    }

    @Override
    public void setObjectFactory(ObjectFactory objectFactory)
    {
        if (!(Callable.class.isAssignableFrom(objectFactory.getObjectClass())))
        {
            throw new MuleRuntimeException(CoreMessages.objectNotOfCorrectType(objectFactory.getObjectClass(),
                Callable.class));
        }
        super.setObjectFactory(objectFactory);
    }
}

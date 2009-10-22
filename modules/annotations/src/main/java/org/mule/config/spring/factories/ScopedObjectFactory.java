/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.factories;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.PoolingProfile;
import org.mule.config.i18n.CoreMessages;
import org.mule.impl.annotations.ServiceScope;
import org.mule.object.AbstractObjectFactory;
import org.mule.object.PrototypeObjectFactory;
import org.mule.object.SingletonObjectFactory;

/** This class is a bastardization of the the exising factories.  It needs to be removed
  */
public class ScopedObjectFactory extends AbstractObjectFactory
{
    private ServiceScope scope = ServiceScope.SINGLETON;

    private PoolingProfile poolingProfile;

    private AbstractObjectFactory delegate;

    private Object objectInstance;

    public ServiceScope getScope()
    {
        return scope;
    }

    public void setScope(ServiceScope scope)
    {
        this.scope = scope;
    }

    public PoolingProfile getPoolingProfile()
    {
        return poolingProfile;
    }

    public void setPoolingProfile(PoolingProfile poolingProfile)
    {
        this.poolingProfile = poolingProfile;
    }


    /**
     * Inform the object factory/container that this object is no longer in use.
     * This may return the object to a pool, deallocate resources, or do something
     * else depending on the implementation. If appropriate, the object will be disposed
     * by this method (Disposable.dispose()).
     */
//    public void release(Object object) throws Exception
//    {
//        if(delegate!=null)
//        {
//            delegate.release(object);
//        }
//        else
//        {
//            throw new IllegalStateException("not initialised");
//        }
//    }

    /**
     * Method used to perform any initialisation work. If a fatal error occurs during
     * initialization, an <code>InitialisationException</code> should be thrown,
     * causing the Mule instance to shut down. If the error is recoverable, say by
     * retrying to connect, a <code>RecoverableException</code> should be thrown.
     * There is no guarantee that by throwing a Recoverable exception that the Mule
     * instance will not shut down.
     *
     * @throws org.mule.umo.lifecycle.InitialisationException
     *          if a fatal error occurs causing the Mule
     *          instance to shutdown
     * @throws org.mule.umo.lifecycle.RecoverableException
     *          if an error occurs that can be recovered from
     */
    public void initialise() throws InitialisationException
    {
        if(scope == ServiceScope.PROTOTYPE)
        {
            delegate = new PrototypeObjectFactory();
        }
        else if(scope == ServiceScope.SINGLETON)
        {
            delegate = new SingletonObjectFactory();
        }
        else if(scope == ServiceScope.POOLED)
        {
            delegate = new PrototypeObjectFactory();
//            delegate = new PooledObjectFactory();
//            if(poolingProfile!=null)
//            {
//                ((PooledObjectFactory) delegate).setPoolingProfile(poolingProfile);
//            }
        }
        else
        {
            throw new InitialisationException(CoreMessages.valueIsInvalidFor(String.valueOf(scope), "scope"), this);
        }
        delegate.setProperties(getProperties());
        delegate.setObjectClass(getObjectClass());
        delegate.initialise();
    }

    /**
     * A lifecycle method where implementor should free up any resources. If an
     * exception is thrown, it should just be logged and processing should continue.
     * This method should not throw Runtime exceptions.
     */
    public void dispose()
    {
        if(delegate!=null)
        {
            delegate.dispose();
        }
    }

    @Override
    public boolean isSingleton()
    {
        return scope == ServiceScope.SINGLETON;
    }

    public Object getObjectInstance()
    {
        return objectInstance;
    }

    public void setObjectInstance(Object instance)
    {
        if(!isSingleton())
        {
            throw new UnsupportedOperationException("cannot set the instance on a non-singleton");
        }
        this.objectInstance = instance;
        setObjectClass(instance.getClass());
    }

    @Override
    public Object getInstance() throws Exception
    {
        if(objectInstance!=null)
        {
            return objectInstance;
        }
        return super.getInstance();
    }
}

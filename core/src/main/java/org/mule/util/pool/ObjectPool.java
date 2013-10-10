/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.pool;

import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.object.ObjectFactory;

/**
 * <code>ObjectPool</code> is a simple pooling interface for objects
 */
public interface ObjectPool extends Initialisable, Disposable
{

    Object borrowObject() throws Exception;

    void returnObject(Object object);

    int getNumActive();

    int getMaxActive();

    void clear();

    void close();

    void setObjectFactory(ObjectFactory objectFactory);
    
    ObjectFactory getObjectFactory();

}

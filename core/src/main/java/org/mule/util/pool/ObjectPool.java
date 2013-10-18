/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.object;


/**
 * <code>ObjectPool</code> is a simple pooling interface for objects
 */
public interface ObjectPool
{
    /**
     * Constants used to determine the exhausted action of the pool
     */
    int WHEN_EXHAUSTED_FAIL = 0;
    /** @deprecated use WHEN_EXHAUSTED_WAIT instead */
    int WHEN_EXHAUSTED_BLOCK = 1;
    int WHEN_EXHAUSTED_WAIT = 1;
    int WHEN_EXHAUSTED_GROW = 2;
    int DEFAULT_EXHAUSTED_ACTION = WHEN_EXHAUSTED_WAIT;

    int DEFAULT_MAX_SIZE = 5;
    int DEFAULT_MAX_WAIT = 4000;

    Object borrowObject() throws Exception;

    void returnObject(Object object) throws Exception;

    int getSize();

    int getMaxSize();

    void setFactory(ObjectFactory factory);

    void clearPool();

    void start() throws Exception;

    void stop() throws Exception;

    void onAdd(Object obj);

    void onRemove(Object obj);

}

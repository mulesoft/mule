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
package org.mule.util;



/**
 * <code>ObjectPool</code> is a simple pooling interface for objects
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface ObjectPool
{
    /**
     * Constants used to determine the exhaused action of the pool
     */
    public static final int WHEN_EXHAUSTED_FAIL = 0;
    public static final int WHEN_EXHAUSTED_BLOCK = 1;
    public static final int WHEN_EXHAUSTED_GROW = 2;

    public static final int DEFAULT_MAX_SIZE = 5;
    public static final int DEFAULT_MAX_WAIT = 4000;
    public static final int DEFAULT_EXHAUSTED_ACTION = WHEN_EXHAUSTED_BLOCK;

    public Object borrowObject() throws Exception;

    public void returnObject(Object object) throws Exception;

    public int getSize();

    public int getMaxSize();

    public void setFactory(ObjectFactory factory);

    public void clearPool();

    public void start() throws Exception;

    public void stop() throws Exception;

    void onAdd(Object obj);

    void onRemove(Object obj);
}

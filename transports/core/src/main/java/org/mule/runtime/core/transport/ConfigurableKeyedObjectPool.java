/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transport;

import org.apache.commons.pool.KeyedObjectPool;

/**
 * A configurable {@link KeyedObjectPool}. Extracted from
 * {@link org.apache.commons.pool.impl.GenericKeyedObjectPool}.
 */
public interface ConfigurableKeyedObjectPool extends KeyedObjectPool
{

    byte WHEN_EXHAUSTED_FAIL = 0;
    byte WHEN_EXHAUSTED_BLOCK = 1;
    byte WHEN_EXHAUSTED_GROW = 2;

    /**
     * Clears the pool, removing all pooled instances.
     */
    void clear();

    /**
     * Returns the overall maximum number of objects (across pools) that can
     * exist at one time. A negative value indicates no limit.
     */
    int getMaxTotal();

    /**
     * Sets the cap on the total number of instances from all pools combined.
     *
     * @param maxTotal The cap on the total number of instances across pools.
     *                 Use a negative value for no limit.
     */
    void setMaxTotal(int maxTotal);

    /**
     * Returns the cap on the number of active instances per key.
     * A negative value indicates no limit.
     */
    int getMaxActive();

    /**
     * Sets the cap on the number of active instances per key.
     *
     * @param maxActive The cap on the number of active instances per key.
     *                  Use a negative value for no limit.
     */
    void setMaxActive(int maxActive);

    /**
     * Returns the maximum amount of time (in milliseconds) the
     * {@link #borrowObject} method should block before throwing
     * an exception when the pool is exhausted and the
     * {@link #setWhenExhaustedAction "when exhausted" action} is
     * {@link #WHEN_EXHAUSTED_BLOCK}.
     * <p/>
     * When less than or equal to 0, the {@link #borrowObject} method
     * may block indefinitely.
     */
    long getMaxWait();

    /**
     * Sets the maximum amount of time (in milliseconds) the
     * {@link #borrowObject} method should block before throwing
     * an exception when the pool is exhausted and the
     * {@link #setWhenExhaustedAction "when exhausted" action} is
     * {@link #WHEN_EXHAUSTED_BLOCK}.
     * <p/>
     * When less than or equal to 0, the {@link #borrowObject} method
     * may block indefinitely.
     *
     * @param maxWait the maximum number of milliseconds borrowObject will block or negative for indefinitely.
     */
    void setMaxWait(long maxWait);

    /**
     * Returns the cap on the number of "idle" instances per key.
     */
    int getMaxIdle();

    /**
     * Sets the cap on the number of "idle" instances in the pool.
     *
     * @param maxIdle the maximum number of "idle" instances that can be held
     *                in a given keyed pool. Use a negative value for no limit.
     */
    void setMaxIdle(int maxIdle);

    /**
     * Sets the action to take when the {@link #borrowObject} method
     * is invoked when the pool is exhausted.
     *
     * @param whenExhaustedAction the action code, which must be one of
     *                            {@link #WHEN_EXHAUSTED_BLOCK}, {@link #WHEN_EXHAUSTED_FAIL},
     *                            or {@link #WHEN_EXHAUSTED_GROW}
     */
    void setWhenExhaustedAction(byte whenExhaustedAction);

    /**
     * Returns the action to take when the {@link #borrowObject} method
     * is invoked when the pool is exhausted.
     *
     * @return one of {@link #WHEN_EXHAUSTED_BLOCK},
     *         {@link #WHEN_EXHAUSTED_FAIL} or {@link #WHEN_EXHAUSTED_GROW}
     */
    byte getWhenExhaustedAction();
}

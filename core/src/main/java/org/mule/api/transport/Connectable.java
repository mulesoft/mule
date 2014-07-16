/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.transport;

import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.retry.RetryContext;

/**
 * Interface for objects that should connect to a resource.
 */
public interface Connectable extends Lifecycle
{

    /**
     * Make the connection to the underlying transport. The fact that this object is
     * connected or not should have no influence on the lifecycle, especially the
     * start / stop state if applicable.
     * 
     * @throws Exception
     */
    void connect() throws Exception;

    /**
     * Disconnect the from the underlying transport
     * 
     * @throws Exception
     */
    void disconnect() throws Exception;

    /**
     * Determines if this object is connected or not
     */
    boolean isConnected();

    /**
     * Returns a string identifying the underlying resource
     */
    String getConnectionDescription();

    /**
     * Test whether the connector is able to connect to its resource(s).
     * This will allow a retry policy to go into effect in the case of failure. Implementations must
     * call either:
     * <ul>
     *  <li>{@link RetryContext#setOk()} when no problems found (or no validation required).
     *  <li>{@link RetryContext#setFailed(Throwable)} with a root cause for a connection failure.
     * </ul>
     * Callers should then check for {@link RetryContext#isOk()}. The failure, if any, will be
     * provided via the {@link RetryContext#getLastFailure()}.
     * 
     * @return same retry context with status info set and any failures populated
     * @throws Exception if the connector fails to connect  @param retryContext
     */
    RetryContext validateConnection(RetryContext retryContext);
}

/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

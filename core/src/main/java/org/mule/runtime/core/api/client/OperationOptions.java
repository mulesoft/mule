/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.client;

/**
 * Base options for every operation executed by {@link org.mule.api.client.MuleClient}
 *
 * Implementations of this class must redefine {@link Object#hashCode()} and {@link java.lang.Object#equals(Object)} since the may be used as key in a map
 */
public interface OperationOptions
{

    /**
     * @return timeout for the operation to execute. May be null if the user didn't configure any.
     */
    Long getResponseTimeout();

}

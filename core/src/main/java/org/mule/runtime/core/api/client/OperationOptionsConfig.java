/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.client;

/**
 *  Common configuration options for all operations
 */
public interface OperationOptionsConfig<BuilderType>
{

    /**
     * @param timeout maximum amount of time to wait for the HTTP response
     * @return the builder
     */
    BuilderType responseTimeout(final long timeout);

}

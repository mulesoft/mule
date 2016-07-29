/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.api.connectivity;

import org.mule.runtime.core.api.MuleRuntimeException;

/**
 * Exception type that represents a failure due to multiple connectivity testing objects
 * present in the mule configuration.
 *
 * @since 4.0
 */
public class MultipleConnectivityTestingObjectsFoundException extends MuleRuntimeException
{

    /**
     * {@inheritDoc}
     */
    public MultipleConnectivityTestingObjectsFoundException(Throwable cause)
    {
        super(cause);
    }

}

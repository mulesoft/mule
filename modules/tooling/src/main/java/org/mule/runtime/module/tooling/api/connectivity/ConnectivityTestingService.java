/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.api.connectivity;

import org.mule.runtime.api.connection.ConnectionValidationResult;

/**
 * Service for doing connectivity testing.
 *
 * A {@code ConnectivityTestingService}
 *
 * @since 4.0
 */
public interface ConnectivityTestingService
{

    /**
     * Does connection testing over the {@code ConnectivityTestingService}.
     *
     * @return connectivity testing result.
     */
    ConnectionValidationResult testConnection();

}

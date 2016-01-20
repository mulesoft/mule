/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.internal.connection;

import org.mule.api.connector.ConnectionManager;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.retry.RetryPolicyTemplate;

/**
 * Interface for {@link ConnectionManager} implementations which expands its contract with non API functionality
 *
 * @since 4.0
 */
public interface ConnectionManagerAdapter extends ConnectionManager, Stoppable
{

    RetryPolicyTemplate getDefaultRetryPolicyTemplate();
}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport;

import org.mule.api.context.WorkManager;

/**
 * Provides a reference to a WorkManager making the client agnostic
 * of the WorkManager lifecycle.
 */
public interface WorkManagerHolder
{

    /**
     * @return work manager that is ready to use.
     */
    WorkManager getWorkManager();

}

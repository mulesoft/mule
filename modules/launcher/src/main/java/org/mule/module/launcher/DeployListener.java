/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher;

import org.mule.module.launcher.application.Application;

/**
 * Defines a lister for deployment events.
 */
public interface DeployListener
{

    /**
     * Notifies that a deploy for a given application has started.
     *
     * @param application the application being deployed
     */
    void onDeployStart(Application application);

    /**
     * Notifies that a deploy for a given application has successfully finished.
     *
     * @param application the application that was deployed
     */
    void onDeploySuccessful(Application application);

    /**
     * Notifies that a deploy for a given application has finished with a failure.
     *
     * @param application the application being deployed
     * @param cause       the cause of the failure
     */
    void onDeployFailure(Application application, Throwable cause);
}

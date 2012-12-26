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

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 *  Manages deploy of mule applications
 */
public interface DeploymentService extends DeploymentListenerManager
{

    /**
     * Finds an active application by name.
     *
     * @return null if not found
     */
    Application findApplication(String appName);

    /**
     * Lists all deployed applications.
     *
     * @return immutable applications list
     */
    List<Application> getApplications();

    void addStartupListener(StartupListener listener);

    void removeStartupListener(StartupListener listener);

    /**
     * Obtains the object used to synchronize the service.
     *
     * @return a non null lock object.
     */
    ReentrantLock getLock();

    void undeploy(String appName);

    void deploy(URL appArchiveUrl) throws IOException;

    void start();

    void stop();
}

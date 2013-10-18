/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.application;

import org.mule.config.i18n.MessageFactory;
import org.mule.module.launcher.DeploymentException;
import org.mule.module.launcher.DeploymentStopException;

import java.io.IOException;

/**
 * Adds a way to simulate errors on application deployment phases
 */
public class TestApplicationWrapper extends ApplicationWrapper
{

    private boolean failOnStopApplication;

    private boolean failOnDisposeApplication;

    protected TestApplicationWrapper(Application delegate) throws IOException
    {
        super(delegate);
    }

    @Override
    public void dispose()
    {
        if (failOnDisposeApplication)
        {
            throw new DeploymentException(MessageFactory.createStaticMessage("Error disposing application"));
        }

        getDelegate().dispose();
    }

    @Override
    public void stop()
    {
        if (failOnStopApplication)
        {
            throw new DeploymentStopException(MessageFactory.createStaticMessage("Error stopping application"));
        }

        getDelegate().stop();
    }

    public void setFailOnStopApplication(boolean failOnStopApplication)
    {
        this.failOnStopApplication = failOnStopApplication;
    }

    public void setFailOnDisposeApplication(boolean failOnDisposeApplication)
    {
        this.failOnDisposeApplication = failOnDisposeApplication;
    }
}

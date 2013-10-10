/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

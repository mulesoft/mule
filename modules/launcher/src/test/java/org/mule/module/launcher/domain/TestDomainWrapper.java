/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.domain;

import org.mule.api.MuleContext;
import org.mule.module.launcher.DeploymentStartException;
import org.mule.module.launcher.InstallException;
import org.mule.module.launcher.artifact.ArtifactClassLoader;

import java.io.File;

public class TestDomainWrapper implements Domain
{

    private Domain delegate;
    private boolean failOnPurpose;
    private boolean failOnDispose;

    public TestDomainWrapper(Domain delegate)
    {
        this.delegate = delegate;
    }

    @Override
    public boolean containsSharedResources()
    {
        return delegate.containsSharedResources();
    }

    @Override
    public MuleContext getMuleContext()
    {
        return delegate.getMuleContext();
    }

    @Override
    public void install() throws InstallException
    {
        delegate.install();
    }

    @Override
    public void init()
    {
        delegate.init();
    }

    @Override
    public void start() throws DeploymentStartException
    {
        delegate.start();
    }

    @Override
    public void stop()
    {
        if (failOnPurpose)
        {
            fail();
        }
        delegate.stop();
    }

    private void fail()
    {
        throw new RuntimeException("fail on purpose");
    }

    @Override
    public void dispose()
    {
        if (failOnDispose)
        {
            fail();
        }
        delegate.dispose();
    }

    @Override
    public void redeploy()
    {
        delegate.redeploy();
    }

    @Override
    public String getArtifactName()
    {
        return delegate.getArtifactName();
    }

    @Override
    public File[] getConfigResourcesFile()
    {
        return delegate.getConfigResourcesFile();
    }

    @Override
    public ArtifactClassLoader getArtifactClassLoader()
    {
        return delegate.getArtifactClassLoader();
    }

    public void setFailOnStop()
    {
        this.failOnPurpose = true;
    }

    public void setFailOnDispose()
    {
        this.failOnDispose = true;
    }
}

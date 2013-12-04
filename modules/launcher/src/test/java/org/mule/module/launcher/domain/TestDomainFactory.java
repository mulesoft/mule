/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.domain;

import java.io.IOException;

public class TestDomainFactory extends DefaultDomainFactory
{

    private boolean failOnStop;
    private boolean failOnDispose;

    public TestDomainFactory(DomainClassLoaderRepository domainClassLoaderRepository)
    {
        super(domainClassLoaderRepository);
    }

    @Override
    public Domain createArtifact(String artifactName) throws IOException
    {
        TestDomainWrapper testDomainWrapper = new TestDomainWrapper(super.createArtifact(artifactName));
        if (this.failOnStop)
        {
            testDomainWrapper.setFailOnStop();
        }
        if (this.failOnDispose)
        {
            testDomainWrapper.setFailOnDispose();
        }
        return testDomainWrapper;
    }

    public void setFailOnStopApplication()
    {
        failOnStop = true;
    }

    public void setFailOnDisposeApplication()
    {
        failOnDispose = true;
    }

}

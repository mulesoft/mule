/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.application;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import org.mule.api.MuleContext;
import org.mule.module.launcher.artifact.ArtifactClassLoader;

import java.net.URL;

import org.junit.Test;
import org.mockito.Mockito;

public class ArtifactMuleContextDelegateTestCase
{

    public static final String DOMAIN = "domain";
    public static final String APPLICATION = "application";

    @Test
    public void useCurrentThreadMuleContext()
    {
        ArtifactMuleContextDelegate artifactMuleContextDelegate = new ArtifactMuleContextDelegate();
        FakeArtifactClassLoader artifactClassLoader = new FakeArtifactClassLoader();
        MuleContext domainMuleContext = mock(MuleContext.class);
        Mockito.when(domainMuleContext.getUniqueIdString()).thenReturn(DOMAIN);
        MuleContext applicationMuleContext = mock(MuleContext.class);
        Mockito.when(applicationMuleContext.getUniqueIdString()).thenReturn(APPLICATION);
        Thread.currentThread().setContextClassLoader(artifactClassLoader);
        artifactClassLoader.muleContext = domainMuleContext;
        assertThat(artifactMuleContextDelegate.getUniqueIdString(), is(DOMAIN));
        artifactClassLoader.muleContext = applicationMuleContext;
        assertThat(artifactMuleContextDelegate.getUniqueIdString(), is(APPLICATION));
    }

    public static class FakeArtifactClassLoader extends ClassLoader implements ArtifactClassLoader
    {

        public MuleContext muleContext;

        @Override
        public URL findResource(String name)
        {
            return super.findResource(name);
        }

        @Override
        public String getArtifactName()
        {
            return "default";
        }

        @Override
        public MuleContext getMuleContext()
        {
            return muleContext;
        }

        @Override
        public ClassLoader getClassLoader()
        {
            return getClass().getClassLoader();
        }
    }

}

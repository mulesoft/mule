/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.mule.module.launcher.artifact.AbstractArtifactClassLoader;
import org.mule.module.launcher.artifact.ResourceReleaser;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.lang.reflect.Method;

import org.junit.Test;

@SmallTest
public class ResourceReleaserTestCase extends AbstractMuleTestCase
{

    public static final String TEST_RESOURCE_RELEASER_CLASS_LOCATION = "/org/mule/module/launcher/TestResourceReleaser.class";

    @Test
    public void createdByCorrectClassLoaderApplication() throws Exception
    {
        ensureResourceReleaserIsCreatedByCorrectClassLoader(new TestMuleApplicationClassLoader(new TestMuleSharedDomainClassLoader()));
    }

    @Test
    public void createdByCorrectClassLoaderDomain() throws Exception
    {
        ensureResourceReleaserIsCreatedByCorrectClassLoader(new TestMuleSharedDomainClassLoader());
    }

    private void ensureResourceReleaserIsCreatedByCorrectClassLoader(AbstractArtifactClassLoader classLoader) throws Exception
    {
        assertThat(classLoader.getClass().getClassLoader(), is(Thread.currentThread().getContextClassLoader()));
        classLoader.setResourceReleaserClassLocation(TEST_RESOURCE_RELEASER_CLASS_LOCATION);
        classLoader.dispose();

        // We must call the getClassLoader method from TestResourceReleaser dynamically in order to not load the
        // class by the current class loader, if not a java.lang.LinkageError is raised.
        ResourceReleaser resourceReleaserInstance = ((KeepResourceReleaserInstance) classLoader).getResourceReleaserInstance();
        Method getClassLoaderMethod = resourceReleaserInstance.getClass().getMethod("getClassLoader");
        ClassLoader resourceReleaserInstanceClassLoader = (ClassLoader) getClassLoaderMethod.invoke(resourceReleaserInstance);

        assertThat(resourceReleaserInstanceClassLoader, is((ClassLoader) classLoader));
    }


    private static interface KeepResourceReleaserInstance
    {

        ResourceReleaser getResourceReleaserInstance();
    }

    private static class TestMuleApplicationClassLoader extends MuleApplicationClassLoader implements KeepResourceReleaserInstance
    {

        private ResourceReleaser resourceReleaserInstance;

        public TestMuleApplicationClassLoader(ClassLoader parentCl)
        {
            super("APP-NAME", parentCl, null);
        }

        @Override
        protected ResourceReleaser createResourceReleaserInstance()
        {
            resourceReleaserInstance = super.createResourceReleaserInstance();
            return resourceReleaserInstance;
        }

        @Override
        public ResourceReleaser getResourceReleaserInstance()
        {
            return resourceReleaserInstance;
        }
    }

    private static class TestMuleSharedDomainClassLoader extends MuleSharedDomainClassLoader implements KeepResourceReleaserInstance
    {

        private ResourceReleaser resourceReleaserInstance;

        public TestMuleSharedDomainClassLoader()
        {
            super("DOMAIN-NAME", Thread.currentThread().getContextClassLoader());
        }

        @Override
        protected void validateAndGetDomainFolders()
        {
            // Ignore validations
        }

        @Override
        protected void addUrls()
        {
            // No urls required
        }

        @Override
        protected ResourceReleaser createResourceReleaserInstance()
        {
            resourceReleaserInstance = super.createResourceReleaserInstance();
            return resourceReleaserInstance;
        }

        @Override
        public ResourceReleaser getResourceReleaserInstance()
        {
            return resourceReleaserInstance;
        }
    }
}


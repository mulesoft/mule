/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.domain;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mule.module.launcher.domain.DomainFactory.DEFAULT_DOMAIN_NAME;
import org.mule.module.artifact.classloader.ArtifactClassLoader;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.mule.module.launcher.MuleFoldersUtil.getDomainsFolder;
import static org.mule.module.launcher.MuleFoldersUtil.getMuleLibFolder;
import static org.mule.module.reboot.MuleContainerBootstrapUtils.MULE_DOMAIN_FOLDER;
import org.mule.module.launcher.DeploymentException;
import org.mule.module.launcher.MuleSharedDomainClassLoader;
import org.mule.module.launcher.descriptor.DomainDescriptor;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Test;

public class MuleDomainClassLoaderRepositoryTestCase extends AbstractDomainTestCase
{

    public MuleDomainClassLoaderRepositoryTestCase() throws IOException
    {
    }

    @After
    public void tearDown()
    {
        deleteIfNeeded(getDomainsFolder());
        deleteIfNeeded(new File(getMuleLibFolder(), "shared"));
    }

    private void deleteIfNeeded(File file)
    {
        if (file.exists())
        {
            deleteQuietly(file);
        }
    }

    @Test
    public void createClassLoaderUsingDefaultDomain()
    {
        createDomainDir(MULE_DOMAIN_FOLDER, DEFAULT_DOMAIN_NAME);
        assertThat(new MuleDomainClassLoaderRepository(lookupPolicy).getDomainClassLoader(DEFAULT_DOMAIN_NAME).getArtifactName(), is(DEFAULT_DOMAIN_NAME));
    }

    @Test
    public void createClassLoaderUsingCustomDomain()
    {
        String domainName = "custom-domain";
        createDomainDir(MULE_DOMAIN_FOLDER, domainName);
        final ArtifactClassLoader domainClassLoader = new MuleDomainClassLoaderRepository(lookupPolicy).getDomainClassLoader(domainName);

        assertThat(domainClassLoader.getClassLoader(), instanceOf(MuleSharedDomainClassLoader.class));
        assertThat(domainClassLoader.getArtifactName(), equalTo(domainName));
    }

    @Test(expected = DeploymentException.class)
    public void validateDomainBeforeCreatingClassLoader()
    {
        new MuleDomainClassLoaderRepository(lookupPolicy).getDomainClassLoader("someDomain");
    }

    @Test
    public void createClassLoaderFromDomainDescriptor()
    {
        String domainName = "descriptor-domain";
        DomainDescriptor descriptor = getTestDescriptor(domainName);
        createDomainDir(MULE_DOMAIN_FOLDER, domainName);
        ArtifactClassLoader domainClassLoader = new MuleDomainClassLoaderRepository(lookupPolicy).getDomainClassLoader(descriptor);

        assertThat(domainClassLoader.getClassLoader(), instanceOf(MuleSharedDomainClassLoader.class));
        assertThat(domainClassLoader.getArtifactName(), equalTo(domainName));
    }

    private DomainDescriptor getTestDescriptor(String name)
    {
        DomainDescriptor descriptor = new DomainDescriptor();
        descriptor.setName(name);
        descriptor.setRedeploymentEnabled(false);
        return descriptor;
    }
}

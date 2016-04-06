/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.domain;

import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mule.module.launcher.MuleFoldersUtil.getDomainsFolder;
import static org.mule.module.launcher.MuleFoldersUtil.getMuleLibFolder;
import static org.mule.module.launcher.MuleSharedDomainClassLoader.OLD_DOMAIN_LIBRARY_FOLDER;
import static org.mule.module.launcher.domain.DomainFactory.DEFAULT_DOMAIN_NAME;
import static org.mule.module.reboot.MuleContainerBootstrapUtils.MULE_DOMAIN_FOLDER;
import org.mule.module.launcher.DeploymentException;
import org.mule.module.launcher.MuleSharedDomainClassLoader;
import org.mule.module.launcher.descriptor.DomainDescriptor;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.hamcrest.core.Is;
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
    public void createClassLoaderUsingEmptyDomain()
    {
        createDomainDir(OLD_DOMAIN_LIBRARY_FOLDER, DEFAULT_DOMAIN_NAME);
        assertThat(new MuleDomainClassLoaderRepository().getDefaultDomainClassLoader().getArtifactName(), Is.is(DEFAULT_DOMAIN_NAME));
    }

    @Test
    public void createClassLoaderUsingDefaultDomain()
    {
        createDomainDir(MULE_DOMAIN_FOLDER, DEFAULT_DOMAIN_NAME);
        assertThat(new MuleDomainClassLoaderRepository().getDomainClassLoader(DEFAULT_DOMAIN_NAME).getArtifactName(), is(DEFAULT_DOMAIN_NAME));
    }

    @Test
    public void createClassLoaderUsingCustomDomain()
    {
        String domainName = "custom-domain";
        createDomainDir(MULE_DOMAIN_FOLDER, domainName);
        assertThat(new MuleDomainClassLoaderRepository().getDomainClassLoader(domainName).getClassLoader(), instanceOf(MuleSharedDomainClassLoader.class));
    }

    @Test(expected = DeploymentException.class)
    public void validateDomainBeforeCreatingClassLoader()
    {
        new MuleDomainClassLoaderRepository().getDomainClassLoader("someDomain");
    }

    @Test
    public void createClassLoaderFromDomainDescriptor()
    {
        String domainName = "descriptor-domain";
        DomainDescriptor descriptor = getTestDescriptor(domainName);

        createDomainDir(MULE_DOMAIN_FOLDER, domainName);
        ClassLoader domainClassLoader = new MuleDomainClassLoaderRepository().getDomainClassLoader(descriptor).getClassLoader();
        assertThat(domainClassLoader, instanceOf(MuleSharedDomainClassLoader.class));
        assertThat(((MuleSharedDomainClassLoader) domainClassLoader).isOverridden("org.mycompany.mymodule.MyClass"), is(true));
        assertThat(((MuleSharedDomainClassLoader) domainClassLoader).isOverridden("org.mycompany.MyClass"), is(false));

    }

    private DomainDescriptor getTestDescriptor(String name)
    {
        DomainDescriptor descriptor = new DomainDescriptor();
        Set<String> domainLoaderOverride = new HashSet<>();
        domainLoaderOverride.add("org.mycompany.mymodule");
        descriptor.setName(name);
        descriptor.setLoaderOverride(domainLoaderOverride);
        return descriptor;
    }

}

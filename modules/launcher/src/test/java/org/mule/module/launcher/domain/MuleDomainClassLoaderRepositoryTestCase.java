/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.domain;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mule.module.launcher.MuleSharedDomainClassLoader.OLD_DOMAIN_LIBRARY_FOLDER;
import static org.mule.module.launcher.domain.DomainFactory.DEFAULT_DOMAIN_NAME;
import static org.mule.module.reboot.MuleContainerBootstrapUtils.MULE_DOMAIN_FOLDER;

import org.mule.api.config.MuleProperties;
import org.mule.module.launcher.DeploymentException;
import org.mule.module.launcher.MuleSharedDomainClassLoader;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.File;
import java.io.IOException;

import org.hamcrest.core.Is;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class MuleDomainClassLoaderRepositoryTestCase extends AbstractMuleTestCase
{

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();
    @Rule
    public final SystemProperty muleHomeSystemProperty = new SystemProperty(MuleProperties.MULE_HOME_DIRECTORY_PROPERTY, temporaryFolder.getRoot().getCanonicalPath());
    private final File muleHomeFolder;

    public MuleDomainClassLoaderRepositoryTestCase() throws IOException
    {
        muleHomeFolder = temporaryFolder.getRoot();
    }

    @Test
    public void createClassLoaderUsingEmptyDomain()
    {
        createOldDomainDefaultDir();
        assertThat(new MuleDomainClassLoaderRepository().getDefaultDomainClassLoader().getArtifactName(), Is.is(DEFAULT_DOMAIN_NAME));
    }

    @Test
    public void createClassLoaderUsingDefaultDomain()
    {
        assertThat(new MuleDomainClassLoaderRepository().getDomainClassLoader(DEFAULT_DOMAIN_NAME).getArtifactName(), is(DEFAULT_DOMAIN_NAME));
    }

    @Test
    public void createClassLoaderUsingCustomDomain()
    {
        String domainName = "custom-domain";
        assertThat(new File(muleHomeFolder, MULE_DOMAIN_FOLDER + File.separator + domainName).mkdirs(), is(true));
        assertThat(new MuleDomainClassLoaderRepository().getDomainClassLoader(domainName).getClassLoader(), instanceOf(MuleSharedDomainClassLoader.class));
    }

    @Test(expected = DeploymentException.class)
    public void validateDomainBeforeCreatingClassLoader()
    {
        new MuleDomainClassLoaderRepository().getDomainClassLoader("someDomain");
    }

    private void createOldDomainDefaultDir()
    {
        assertThat(new File(muleHomeFolder, OLD_DOMAIN_LIBRARY_FOLDER + File.separator + "default").mkdirs(), is(true));
    }


}

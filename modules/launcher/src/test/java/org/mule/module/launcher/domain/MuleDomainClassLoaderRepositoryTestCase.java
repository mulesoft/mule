/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.domain;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mule.module.launcher.domain.DomainFactory.DEFAULT_DOMAIN_NAME;
import org.mule.api.config.MuleProperties;
import org.mule.module.artifact.classloader.ArtifactClassLoader;
import org.mule.module.artifact.classloader.ClassLoaderLookupPolicy;
import org.mule.module.artifact.classloader.MuleClassLoaderLookupPolicy;
import org.mule.module.launcher.DeploymentException;
import org.mule.module.launcher.MuleFoldersUtil;
import org.mule.module.launcher.MuleSharedDomainClassLoader;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.File;
import java.io.IOException;

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
    private final ClassLoaderLookupPolicy lookupPolicy = new MuleClassLoaderLookupPolicy(emptyMap(), emptySet());

    public MuleDomainClassLoaderRepositoryTestCase() throws IOException
    {
        muleHomeFolder = temporaryFolder.getRoot();
    }

    @Test
    public void createClassLoaderUsingDefaultDomain()
    {
        assertThat(new MuleDomainClassLoaderRepository(lookupPolicy).getDomainClassLoader(DEFAULT_DOMAIN_NAME).getArtifactName(), is(DEFAULT_DOMAIN_NAME));
    }

    @Test
    public void createClassLoaderUsingCustomDomain()
    {
        String domainName = "custom-domain";
        final File domainFolder = MuleFoldersUtil.getDomainFolder(domainName);
        assertThat(domainFolder.mkdirs(), is(true));

        final ArtifactClassLoader domainClassLoader = new MuleDomainClassLoaderRepository(lookupPolicy).getDomainClassLoader(domainName);

        assertThat(domainClassLoader.getClassLoader(), instanceOf(MuleSharedDomainClassLoader.class));
        assertThat(domainClassLoader.getArtifactName(), equalTo(domainName));
    }

    @Test(expected = DeploymentException.class)
    public void validateDomainBeforeCreatingClassLoader()
    {
        new MuleDomainClassLoaderRepository(lookupPolicy).getDomainClassLoader("someDomain");
    }
}

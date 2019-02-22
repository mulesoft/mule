/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import org.mule.api.config.MuleProperties;
import org.mule.module.reboot.MuleContainerBootstrapUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.size.SmallTest;
import org.mule.util.FileUtils;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SmallTest
public class MuleSharedDomainClassLoaderTestCase extends AbstractMuleTestCase
{

    public static final String RESOURCE_FILE_NAME = "file.properties";
    public static final String DEFAULT_DOMAIN_NAME = "default";
    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();
    @Rule
    public final SystemProperty muleHomeSystemProperty = new SystemProperty(MuleProperties.MULE_HOME_DIRECTORY_PROPERTY, temporaryFolder.getRoot().getCanonicalPath());
    private final File muleHomeFolder;

    public MuleSharedDomainClassLoaderTestCase() throws IOException
    {
        muleHomeFolder = temporaryFolder.getRoot();
    }

    @Before
    public void setUp() throws IOException
    {
        temporaryFolder.delete();
        temporaryFolder.create();
    }

    @Test
    public void whenOldDomainExistsUseOldDomain() throws Exception
    {
        createOldDomainFolder(DEFAULT_DOMAIN_NAME);
        createDomainFolder(DEFAULT_DOMAIN_NAME);
        createOldDomainResource(DEFAULT_DOMAIN_NAME, RESOURCE_FILE_NAME);
        MuleSharedDomainClassLoader classLoader = new MuleSharedDomainClassLoader(DEFAULT_DOMAIN_NAME, getClass().getClassLoader());
        assertThat(classLoader.findResource(RESOURCE_FILE_NAME), notNullValue());
    }

    @Test
    public void whenOldDomainDonNotExistsUseDomainFolder() throws Exception
    {
        createDomainFolder(DEFAULT_DOMAIN_NAME);
        createDomainResource(DEFAULT_DOMAIN_NAME, RESOURCE_FILE_NAME);
        MuleSharedDomainClassLoader classLoader = new MuleSharedDomainClassLoader(DEFAULT_DOMAIN_NAME, getClass().getClassLoader());
        assertThat(classLoader.findResource(RESOURCE_FILE_NAME), notNullValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void failIfDomainFolderCanNotBeCreated() throws Exception
    {
        System.setProperty(MuleProperties.MULE_HOME_DIRECTORY_PROPERTY, "not a valid folder");
        new MuleSharedDomainClassLoader(DEFAULT_DOMAIN_NAME, getClass().getClassLoader());
    }


    private void createOldDomainResource(String domainName, String resourceFile) throws Exception
    {
        assertThat(FileUtils.createFile(new File(getOldDomainFolder(domainName), resourceFile).getAbsolutePath()).exists(), is(true));
    }

    private void createDomainResource(String domainName, String resourceFile) throws Exception
    {
        assertThat(FileUtils.createFile(new File(getDomainFolder(domainName), resourceFile).getAbsolutePath()).exists(), is(true));
    }

    private void createDomainFolder(String domainName)
    {
        assertThat(getDomainFolder(domainName).mkdirs(), is(true));
    }

    private File getDomainFolder(String domainName)
    {
        return new File(muleHomeFolder, MuleContainerBootstrapUtils.MULE_DOMAIN_FOLDER + File.separator + domainName);  //To change body of created methods use File | Settings | File Templates.
    }

    private File getOldDomainFolder(String domainName)
    {
        return new File(muleHomeFolder, MuleSharedDomainClassLoader.DOMAIN_LIBRARY_FOLDER + File.separator + "shared" + File.separator + domainName);
    }

    private void createOldDomainFolder(String domainName)
    {
        assertThat(getOldDomainFolder(domainName).mkdirs(), is(true));
    }
}

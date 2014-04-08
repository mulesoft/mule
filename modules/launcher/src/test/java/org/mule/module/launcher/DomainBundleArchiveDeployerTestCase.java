/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mule.api.config.MuleProperties.MULE_HOME_DIRECTORY_PROPERTY;

import org.mule.module.launcher.domain.Domain;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.util.FileUtils;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class DomainBundleArchiveDeployerTestCase extends AbstractMuleTestCase
{

    public static final String DOMAIN_ZIP_PATH = "someZipFile";
    public static final String DOMAIN_NAME = "domain-name";
    public static final String ZIP_FILE_EXTENSION = ".zip";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private ArchiveDeployer<Domain> mockArchiveDeployer;
    @Mock
    private Domain mockDomain;
    private File domainsFolder;
    private File appsFolder;

    @Before
    public void setUp()
    {
        System.setProperty(MULE_HOME_DIRECTORY_PROPERTY, temporaryFolder.getRoot().getAbsolutePath());
        domainsFolder = new File(temporaryFolder.getRoot(), "domains");
        domainsFolder.mkdirs();
        appsFolder = new File(temporaryFolder.getRoot(), "apps");
        appsFolder.mkdirs();
        when(mockArchiveDeployer.getDeploymentDirectory()).thenReturn(domainsFolder);
        when(mockDomain.getArtifactName()).thenReturn(DOMAIN_NAME);
    }

    @After
    public void tearDown()
    {
        System.clearProperty(MULE_HOME_DIRECTORY_PROPERTY);
    }

    @Test
    public void returnNullIfDeploymentReturnsNull()
    {
        when(mockArchiveDeployer.deployPackagedArtifact(DOMAIN_ZIP_PATH)).thenReturn(null);
        DomainBundleArchiveDeployer domainBundleArchiveDeployer = new DomainBundleArchiveDeployer(mockArchiveDeployer);
        assertThat(domainBundleArchiveDeployer.deployPackagedArtifact("someZipFile"), nullValue());
    }

    @Test
    public void doNotFailIfNoAppsFolderPresent() throws Exception
    {
        when(mockArchiveDeployer.deployPackagedArtifact(DOMAIN_ZIP_PATH)).thenReturn(mockDomain);
        DomainBundleArchiveDeployer domainBundleArchiveDeployer = new DomainBundleArchiveDeployer(mockArchiveDeployer);
        assertThat(domainBundleArchiveDeployer.deployPackagedArtifact(DOMAIN_ZIP_PATH), is(mockDomain));
    }

    @Test
    public void deployAllAppsInsideDomainAppsFolder() throws Exception
    {
        String testAppName = "test-app";
        String testApp2Name = "test-app2";
        when(mockArchiveDeployer.deployPackagedArtifact(DOMAIN_ZIP_PATH)).thenReturn(mockDomain);
        putApplicationInTestDomainAppsFolder(testAppName);
        putApplicationInTestDomainAppsFolder(testApp2Name);
        DomainBundleArchiveDeployer domainBundleArchiveDeployer = new DomainBundleArchiveDeployer(mockArchiveDeployer);
        domainBundleArchiveDeployer.deployPackagedArtifact(DOMAIN_ZIP_PATH);
        verifyApplicationCopyToAppsFolder(testAppName);
        verifyApplicationCopyToAppsFolder( testApp2Name);
    }

    private void verifyApplicationCopyToAppsFolder(String applicationName)
    {
        assertThat(new File(appsFolder, applicationName + ZIP_FILE_EXTENSION).exists(), is(true));
    }

    private void putApplicationInTestDomainAppsFolder(String appName) throws IOException
    {
        File domainDirectory = new File(domainsFolder, DOMAIN_NAME);
        domainDirectory.mkdirs();
        assertThat(FileUtils.createFile(new File(domainsFolder, DOMAIN_NAME + File.separator + DomainBundleArchiveDeployer.DOMAIN_BUNDLE_APPS_FOLDER + File.separator + appName + ZIP_FILE_EXTENSION).getAbsolutePath()).exists(), is(true));
    }
}

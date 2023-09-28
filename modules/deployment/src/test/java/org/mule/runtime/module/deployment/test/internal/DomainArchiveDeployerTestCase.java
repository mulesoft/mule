/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.test.internal;

import static org.mule.runtime.core.api.config.MuleProperties.MULE_HOME_DIRECTORY_PROPERTY;
import static org.mule.test.allure.AllureConstants.ArtifactDeploymentFeature.DOMAIN_DEPLOYMENT;

import static java.util.Optional.empty;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.api.util.FileUtils;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;
import org.mule.runtime.module.deployment.api.DeploymentService;
import org.mule.runtime.module.deployment.internal.ArchiveDeployer;
import org.mule.runtime.module.deployment.internal.ArtifactDeployer;
import org.mule.runtime.module.deployment.internal.DomainArchiveDeployer;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import io.qameta.allure.Feature;

@SmallTest
@Feature(DOMAIN_DEPLOYMENT)
public class DomainArchiveDeployerTestCase extends AbstractMuleTestCase {

  public static final String DOMAIN_ZIP_PATH = "someZipFile";
  public static final String DOMAIN_NAME = "domain-name";
  public static final String JAR_FILE_EXTENSION = ".jar";
  public static final String MOCK_APPLICATION_1_NAME = "MOCK_APPLICATION1_NAME";
  public static final String MOCK_APPLICATION_2_NAME = "MOCK_APPLICATION2_NAME";
  public static final String NON_EXISTENT_DOMAIN_ID = "NonExistentDomainId";

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Mock(lenient = true)
  private ArchiveDeployer<DomainDescriptor, Domain> mockDomainDeployer;

  @Mock(lenient = true)
  private Domain mockDomain;

  @Mock(lenient = true)
  private ArchiveDeployer<ApplicationDescriptor, Application> mockApplicationDeployer;

  @Mock(lenient = true)
  private DeploymentService mockDeploymentService;

  @Mock(lenient = true)
  private Application mockApplication1;

  @Mock(lenient = true)
  private Application mockApplication2;

  @Mock(lenient = true)
  private ArtifactDeployer<Application> mockApplicationArtifactDeployer;

  private File domainsFolder;
  private File appsFolder;

  @Before
  public void setUp() {
    System.setProperty(MULE_HOME_DIRECTORY_PROPERTY, temporaryFolder.getRoot().getAbsolutePath());
    domainsFolder = new File(temporaryFolder.getRoot(), "domains");
    domainsFolder.mkdirs();
    appsFolder = new File(temporaryFolder.getRoot(), "apps");
    appsFolder.mkdirs();
    when(mockDomainDeployer.getDeploymentDirectory()).thenReturn(domainsFolder);
    when(mockDomain.getArtifactName()).thenReturn(DOMAIN_NAME);
    when(mockApplication1.getArtifactName()).thenReturn(MOCK_APPLICATION_1_NAME);
    when(mockApplication2.getArtifactName()).thenReturn(MOCK_APPLICATION_2_NAME);
  }

  @After
  public void tearDown() {
    System.clearProperty(MULE_HOME_DIRECTORY_PROPERTY);
  }

  @Test
  public void returnNullIfDeploymentReturnsNull() {
    when(mockDomainDeployer.deployPackagedArtifact(DOMAIN_ZIP_PATH, empty())).thenReturn(null);
    DomainArchiveDeployer domainArchiveDeployer =
        new DomainArchiveDeployer(mockDomainDeployer, mockApplicationDeployer, mockDeploymentService);
    assertThat(domainArchiveDeployer.deployPackagedArtifact("someZipFile", empty()), nullValue());
  }

  @Test
  public void doNotFailIfNoAppsFolderPresent() throws Exception {
    when(mockDomainDeployer.deployPackagedArtifact(DOMAIN_ZIP_PATH, empty())).thenReturn(mockDomain);
    DomainArchiveDeployer domainArchiveDeployer =
        new DomainArchiveDeployer(mockDomainDeployer, mockApplicationDeployer, mockDeploymentService);
    assertThat(domainArchiveDeployer.deployPackagedArtifact(DOMAIN_ZIP_PATH, empty()), is(mockDomain));
  }

  @Test
  public void undeployDomainWithNoApps() throws Exception {
    when(mockDeploymentService.findDomain(DOMAIN_NAME)).thenReturn(mockDomain);
    when(mockDomainDeployer.deployPackagedArtifact(DOMAIN_ZIP_PATH, empty())).thenReturn(mockDomain);
    when(mockDeploymentService.findDomainApplications(DOMAIN_NAME)).thenReturn(Arrays.asList(new Application[0]));
    DomainArchiveDeployer domainArchiveDeployer =
        new DomainArchiveDeployer(mockDomainDeployer, mockApplicationDeployer, mockDeploymentService);
    domainArchiveDeployer.undeployArtifact(DOMAIN_NAME);
    verify(mockDomainDeployer, times(1)).undeployArtifact(DOMAIN_NAME);
  }

  @Test
  public void undeployDomainApps() throws Exception {
    when(mockDeploymentService.findDomain(DOMAIN_NAME)).thenReturn(mockDomain);
    when(mockDomainDeployer.deployPackagedArtifact(DOMAIN_ZIP_PATH, empty())).thenReturn(mockDomain);
    when(mockDeploymentService.findDomainApplications(DOMAIN_NAME))
        .thenReturn(Arrays.asList(new Application[] {mockApplication1, mockApplication2}));
    DomainArchiveDeployer domainArchiveDeployer =
        new DomainArchiveDeployer(mockDomainDeployer, mockApplicationDeployer, mockDeploymentService);
    domainArchiveDeployer.undeployArtifact(DOMAIN_NAME);
    verify(mockApplicationDeployer, times(1)).undeployArtifact(MOCK_APPLICATION_1_NAME);
    verify(mockApplicationDeployer, times(1)).undeployArtifact(MOCK_APPLICATION_2_NAME);
    verify(mockDomainDeployer, times(1)).undeployArtifact(DOMAIN_NAME);
  }

  @Test(expected = IllegalArgumentException.class)
  public void undeployNonExistentDomain() {
    when(mockDeploymentService.findDomain(NON_EXISTENT_DOMAIN_ID)).thenReturn(null);
    DomainArchiveDeployer domainArchiveDeployer =
        new DomainArchiveDeployer(mockDomainDeployer, mockApplicationDeployer, mockDeploymentService);
    domainArchiveDeployer.undeployArtifact(NON_EXISTENT_DOMAIN_ID);
  }

  private void verifyApplicationCopyToAppsFolder(String applicationName) {
    assertThat(new File(appsFolder, applicationName + JAR_FILE_EXTENSION).exists(), is(true));
  }

  private void putApplicationInTestDomainAppsFolder(String appName) throws IOException {
    File domainDirectory = new File(domainsFolder, DOMAIN_NAME);
    domainDirectory.mkdirs();
    assertThat(FileUtils.createFile(new File(domainsFolder,
                                             DOMAIN_NAME + File.separator + DomainArchiveDeployer.DOMAIN_BUNDLE_APPS_FOLDER
                                                 + File.separator + appName + JAR_FILE_EXTENSION).getAbsolutePath())
        .exists(), is(true));
  }
}

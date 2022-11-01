/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.policy;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.POLICY;
import static org.mule.runtime.core.api.util.FileUtils.stringToFile;
import static org.mule.runtime.module.deployment.impl.internal.policy.FileSystemPolicyClassLoaderConfigurationLoader.LIB_DIR;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.CLASSLOADING_ISOLATION;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.ClassloadingIsolationStory.CLASSLOADER_CONFIGURATION;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.ClassloadingIsolationStory.CLASSLOADER_CONFIGURATION_LOADER;

import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;

import io.qameta.allure.Feature;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@Feature(CLASSLOADING_ISOLATION)
@Stories({@Story(CLASSLOADER_CONFIGURATION_LOADER), @Story(CLASSLOADER_CONFIGURATION)})
public class FileSystemPolicyClassLoaderConfigurationLoaderTestCase extends AbstractMuleTestCase {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private final FileSystemPolicyClassLoaderConfigurationLoader classLoaderConfigurationLoader =
      new FileSystemPolicyClassLoaderConfigurationLoader();

  @Test
  public void createsClassLoaderConfigurationFromFolder() throws Exception {
    File policyFolder = temporaryFolder.newFolder();
    File libFolder = new File(policyFolder, LIB_DIR);
    assertThat(libFolder.mkdir(), is(true));

    File file1 = new File(libFolder, "test1.jar");
    stringToFile(file1.getAbsolutePath(), "foo");
    File file2 = new File(libFolder, "test2.jar");
    stringToFile(file2.getAbsolutePath(), "foo");

    ClassLoaderConfiguration classLoaderConfiguration = classLoaderConfigurationLoader.load(policyFolder, null, POLICY);

    assertThat(classLoaderConfiguration.getUrls().length, equalTo(3));
    assertThat(classLoaderConfiguration.getUrls()[0], equalTo(policyFolder.toURI().toURL()));
    assertThat(asList(classLoaderConfiguration.getUrls()), allOf(hasItem(file1.toURI().toURL()), hasItem(file2.toURI().toURL())));
    assertThat(classLoaderConfiguration.getDependencies(), is(empty()));
    assertThat(classLoaderConfiguration.getExportedPackages(), is(empty()));
    assertThat(classLoaderConfiguration.getExportedResources(), is(empty()));
  }
}

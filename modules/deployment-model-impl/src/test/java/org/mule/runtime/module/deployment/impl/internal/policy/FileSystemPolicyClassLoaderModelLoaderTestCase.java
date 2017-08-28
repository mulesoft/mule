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
import static org.mule.runtime.module.deployment.impl.internal.policy.FileSystemPolicyClassLoaderModelLoader.LIB_DIR;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FileSystemPolicyClassLoaderModelLoaderTestCase extends AbstractMuleTestCase {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private final FileSystemPolicyClassLoaderModelLoader classLoaderModelLoader = new FileSystemPolicyClassLoaderModelLoader();

  @Test
  public void createsClassLoaderModelFromFolder() throws Exception {
    File policyFolder = temporaryFolder.newFolder();
    File libFolder = new File(policyFolder, LIB_DIR);
    assertThat(libFolder.mkdir(), is(true));

    File file1 = new File(libFolder, "test1.jar");
    stringToFile(file1.getAbsolutePath(), "foo");
    File file2 = new File(libFolder, "test2.jar");
    stringToFile(file2.getAbsolutePath(), "foo");

    ClassLoaderModel classLoaderModel = classLoaderModelLoader.load(policyFolder, null, POLICY);

    assertThat(classLoaderModel.getUrls().length, equalTo(3));
    assertThat(classLoaderModel.getUrls()[0], equalTo(policyFolder.toURI().toURL()));
    assertThat(asList(classLoaderModel.getUrls()), allOf(hasItem(file1.toURI().toURL()), hasItem(file2.toURI().toURL())));
    assertThat(classLoaderModel.getDependencies(), is(empty()));
    assertThat(classLoaderModel.getExportedPackages(), is(empty()));
    assertThat(classLoaderModel.getExportedResources(), is(empty()));
  }
}

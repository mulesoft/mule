/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.classloading;

import static org.mule.runtime.api.component.location.Location.builder;
import static org.mule.tck.util.TestConnectivityUtils.disableAutomaticTestConnectivity;
import static org.mule.test.classloading.CLKeysResolver.GET_METADATA;
import static org.mule.test.classloading.api.ClassLoadingHelper.verifyUsedClassLoaders;

import org.mule.runtime.api.metadata.MetadataService;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.classloading.api.ClassLoadingHelper;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import jakarta.inject.Inject;

import org.junit.Rule;
import org.junit.Test;

public class ClassLoadingOnMetadataTestCase extends AbstractExtensionFunctionalTestCase {

  @Inject
  private MetadataService metadataService;

  @Rule
  public SystemProperty disableTestConnectivity = disableAutomaticTestConnectivity();

  @Override
  protected void doTearDown() throws Exception {
    ClassLoadingHelper.createdClassLoaders.clear();
  }

  @Override
  protected String getConfigFile() {
    return "classloading/classloading-extension-config.xml";
  }

  @Test
  public void operationWithMetadataResolver() throws Exception {
    metadataService.getMetadataKeys(builder().globalName("none").build());
    verifyUsedClassLoaders(GET_METADATA);
  }

  @Override
  public boolean addToolingObjectsToRegistry() {
    return true;
  }
}

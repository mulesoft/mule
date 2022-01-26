/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

import javax.inject.Inject;

import org.junit.Test;

@ArtifactClassLoaderRunnerConfig(applicationSharedRuntimeLibs = {"org.mule.tests:mule-tests-model"})
public class ApplicationAsExtensionLoaderTestCase extends MuleArtifactFunctionalTestCase {

  @Inject
  private ExtensionManager extensionManager;

  @Override
  protected String getConfigFile() {
    return "app-as-mule-extension.xml";
  }

  @Test
  public void loadApplicationExtensionModel() throws Exception {
    ExtensionModel extensionModel = extensionManager.getExtension(muleContext.getConfiguration().getId()).get();
    assertThat(extensionModel.getOperationModels(), hasSize(1));
  }
}

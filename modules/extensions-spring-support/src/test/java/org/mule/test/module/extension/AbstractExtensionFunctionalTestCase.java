/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.module.extension;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

import java.util.Optional;

@ArtifactClassLoaderRunnerConfig(applicationSharedRuntimeLibs = {"org.mule.tests:mule-tests-model"})
public abstract class AbstractExtensionFunctionalTestCase extends MuleArtifactFunctionalTestCase {

  protected Optional<ExtensionModel> getExtensionModel(String name) {
    return muleContext.getExtensionManager().getExtension(name);
  }
}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.module.extension;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

import java.util.Optional;

@ArtifactClassLoaderRunnerConfig(
    plugins = {"org.mule.tests:mule-heisenberg-extension", "org.mule.tests:mule-vegan-extension",
        "org.mule.tests:mule-petstore-extension", "org.mule.tests:mule-metadata-extension",
        "org.mule.tests:mule-marvel-extension", "org.mule.tests:mule-subtypes-extension",
        "org.mule.tests:mule-tx-extension", "org.mule.tests:mule-some-extension",
        "org.mule.tests:mule-implicit-config-extension", "org.mule.tests:mule-typed-value-extension",
        "org.mule.tests:mule-parameter-resolver-extension", "org.mule.tests:mule-implicit-exclusive-config-extension",
        "org.mule.tests:mule-multi-implicit-config-extension"},
    testExclusions = {"org.mule.tests:mule-heisenberg-extension:*:*:*", "org.mule.tests:mule-vegan-extension:*:*:*",
        "org.mule.tests:mule-petstore-extension:*:*:*", "org.mule.tests:mule-metadata-extension:*:*:*",
        "org.mule.tests:mule-marvel-extension:*:*:*", "org.mule.tests:mule-subtypes-extension:*:*:*",
        "org.mule.tests:mule-tx-extension:*:*:*", "org.mule.tests:mule-some-extension:*:*:*",
        "org.mule.tests:mule-implicit-config-extension:*:*:*", "org.mule.tests:mule-typed-value-extension:*:*:*",
        "org.mule.tests:mule-parameter-resolver-extension:*:*:*",
        "org.mule.tests:mule-implicit-exclusive-config-extension:*:*:*",
        "org.mule.tests:mule-multi-implicit-config-extension:*:*:*"},
    sharedRuntimeLibs = {"org.mule.tests:mule-tests-unit"})
public class AbstractExtensionFunctionalTestCase extends MuleArtifactFunctionalTestCase {

  protected Optional<ExtensionModel> getExtensionModel(String name) {
    return muleContext.getExtensionManager().getExtension(name);
  }
}

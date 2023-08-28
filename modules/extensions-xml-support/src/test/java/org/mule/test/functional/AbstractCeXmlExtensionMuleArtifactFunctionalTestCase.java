/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.functional;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

/**
 * Abstract class to generate an {@link ExtensionModel} from an extension built from an XML file.
 *
 * @since 4.3
 */
@ArtifactClassLoaderRunnerConfig(applicationSharedRuntimeLibs = {
    "org.mule.tests:mule-activemq-broker"
})
public abstract class AbstractCeXmlExtensionMuleArtifactFunctionalTestCase
    extends AbstractXmlExtensionMuleArtifactFunctionalTestCase {

}

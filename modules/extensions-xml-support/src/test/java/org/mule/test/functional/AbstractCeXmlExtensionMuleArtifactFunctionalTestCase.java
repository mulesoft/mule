/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

/**
 * Abstract class to generate an {@link ExtensionModel} from an extension built from an XML file.
 *
 * @since 4.0
 */
@ArtifactClassLoaderRunnerConfig(applicationSharedRuntimeLibs = {"org.apache.activemq:activemq-client",
    "org.apache.activemq:activemq-broker", "org.apache.activemq:activemq-kahadb-store", "org.fusesource.hawtbuf:hawtbuf"})
public abstract class AbstractCeXmlExtensionMuleArtifactFunctionalTestCase
    extends AbstractXmlExtensionMuleArtifactFunctionalTestCase {

}

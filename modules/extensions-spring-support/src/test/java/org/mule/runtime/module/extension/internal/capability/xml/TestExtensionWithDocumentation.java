/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml;

import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;

/**
 * This is some documentation.
 */
@Extension(name = "documentation", description = "Test Extension Description")
@Configurations({TestDocumentedConfig.class, TestAnotherDocumentedConfig.class})
@Xml(namespace = "namespaceLocation", prefix = "documentation")
public class TestExtensionWithDocumentation {

}

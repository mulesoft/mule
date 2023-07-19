/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.capability.xml.extension.multiple.config;

import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;

/**
 * Test Extension Description
 */
@Extension(name = "multiple")
@Configurations({TestDocumentedConfig.class, TestAnotherDocumentedConfig.class})
@Xml(namespace = "namespaceLocation", prefix = "documentation")
public class TestExtensionWithDocumentationAndMultipleConfig {

}

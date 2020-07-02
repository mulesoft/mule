package org.mule.tests.api;

import org.mule.runtime.extension.api.annotation.Export;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.tests.internal.QueueConfiguration;
import org.mule.tests.internal.SkeletonSource;


/**
 * This is the main class of an extension, is the entry point from which configurations, connection providers, operations
 * and sources are going to be declared.
 */
@Xml(prefix = "test-components")
@Extension(name = "Test Components")
@Sources(SkeletonSource.class)
@Configurations(QueueConfiguration.class)
@Export(classes = TestQueueManager.class)
public class TestComponentsExtension {
}

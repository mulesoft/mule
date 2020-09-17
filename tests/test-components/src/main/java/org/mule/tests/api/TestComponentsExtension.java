/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tests.api;

import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Export;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.extension.api.annotation.privileged.DeclarationEnrichers;
import org.mule.tests.api.pojos.DependencyInjectionObject;
import org.mule.tests.api.pojos.ElementWithAttributeAndChild;
import org.mule.tests.api.pojos.LifecycleObject;
import org.mule.tests.api.pojos.MyPojo;
import org.mule.tests.api.pojos.ParameterCollectionParser;
import org.mule.tests.api.pojos.SameChildTypeContainer;
import org.mule.tests.api.pojos.TextPojo;
import org.mule.tests.internal.QueueConfiguration;
import org.mule.tests.internal.SkeletonSource;

/**
 * This is the main class of an extension, is the entry point from which configurations, connection providers, operations
 * and sources are going to be declared.
 */
@Xml(prefix = "test-components")
@Extension(name = "Test Components")
@Sources(SkeletonSource.class)
@DeclarationEnrichers(LifecycleTrackerEnricher.class)
@Configurations(value = {QueueConfiguration.class, LifecycleTrackerConfiguration.class})
@Export(classes = {TestQueueManager.class, ParameterCollectionParser.class, ElementWithAttributeAndChild.class,
        TextPojo.class, MyPojo.class, SameChildTypeContainer.class, DependencyInjectionObject.class,
        LifecycleObject.class})
public class TestComponentsExtension {
}

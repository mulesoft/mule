/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.resource.extension;

import static org.mule.sdk.api.meta.JavaVersion.JAVA_17;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_21;

import org.mule.runtime.extension.api.annotation.Export;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.sdk.api.annotation.JavaVersionSupport;

/**
 * This extension exposes operations to validate resource access from a plugin and service, but also exposes a resource to
 * validate exported ones can in fact be accessed.
 */
@Extension(name = "ResourceExtension")
@JavaVersionSupport({JAVA_21, JAVA_17})
@Xml(prefix = "rs")
@Operations(ResourceOps.class)
@Export(resources = {"stuff.json"})
public class ResourceExtension {

}

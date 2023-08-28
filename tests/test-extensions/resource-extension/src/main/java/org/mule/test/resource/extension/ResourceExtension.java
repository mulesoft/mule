/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.resource.extension;

import org.mule.runtime.extension.api.annotation.Export;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;

/**
 * This extension exposes operations to validate resource access from a plugin and service, but also exposes a resource to
 * validate exported ones can in fact be accessed.
 */
@Extension(name = "ResourceExtension")
@Xml(prefix = "rs")
@Operations(ResourceOps.class)
@Export(resources = {"stuff.json"})
public class ResourceExtension {

}

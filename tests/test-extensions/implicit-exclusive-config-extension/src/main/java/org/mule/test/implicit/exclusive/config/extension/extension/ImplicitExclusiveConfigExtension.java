/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.implicit.exclusive.config.extension.extension;

import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Export;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;

@Extension(name = "implicitExclusive")
@Xml(namespace = "http://www.mulesoft.org/schema/mule/implicitexclusive", prefix = "implicitexclusive")
@Configurations(value = {BlaConfig.class, BleConfig.class, NonImplicitConfig.class, ImplicitConfigWithOptionalParameter.class})
@Export(classes = ConfigWithNumber.class)
public class ImplicitExclusiveConfigExtension {
}

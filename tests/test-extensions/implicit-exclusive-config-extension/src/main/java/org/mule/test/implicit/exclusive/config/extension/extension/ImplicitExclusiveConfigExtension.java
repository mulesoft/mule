/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.implicit.exclusive.config.extension.extension;

import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Export;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;

@Extension(name = "implicitExclusive")
@Xml(namespace = "http://www.mulesoft.org/schema/mule/implicitexclusive", prefix = "implicitexclusive")
@Configurations(value = {BlaConfig.class, BleConfig.class, NonImplicitConfig.class})
@Export(classes = ConfigWithNumber.class)
public class ImplicitExclusiveConfigExtension {
}

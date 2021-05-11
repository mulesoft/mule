/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.some.extension;

import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Export;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.extension.api.annotation.error.ErrorTypes;
import org.mule.test.heisenberg.extension.HeisenbergErrors;

@Extension(name = "SomeExtension")
@Configurations({SomeConfiguration.class, ParameterGroupConfiguration.class})
@ErrorTypes(HeisenbergErrors.class)
@Export(classes = CustomConnectionException.class)
@Xml(namespace = "http://www.mulesoft.org/schema/mule/some", prefix = "some")
public class SomeExtension {
}

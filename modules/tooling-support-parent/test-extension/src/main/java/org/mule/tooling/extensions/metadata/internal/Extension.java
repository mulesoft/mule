/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tooling.extensions.metadata.internal;

import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.tooling.extensions.metadata.internal.config.SimpleConfiguration;
import org.mule.tooling.extensions.metadata.internal.connection.TstConnectionProvider;

@org.mule.runtime.extension.api.annotation.Extension(name = "ToolingSupportTest", vendor = "MuleSoft Inc.")
@Configurations({SimpleConfiguration.class})
@ConnectionProviders({TstConnectionProvider.class})
@Xml(prefix = "tst")
public class Extension {

}

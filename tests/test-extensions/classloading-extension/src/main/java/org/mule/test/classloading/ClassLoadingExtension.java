/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.classloading;

import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Export;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.test.classloading.api.ClassLoadingHelper;

@Extension(name = "ClassLoading")
@Xml(prefix = "classloading")
@ConnectionProviders({CLPoolingConnectionProvider.class, CLCachedConnectionProvider.class, CLNoneConnectionProvider.class})
@Configurations(CLConfiguration.class)
@Export(classes = ClassLoadingHelper.class)
public class ClassLoadingExtension {

}

/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.classloading;

import static org.mule.sdk.api.meta.JavaVersion.JAVA_17;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_21;

import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Export;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.sdk.api.annotation.JavaVersionSupport;
import org.mule.test.classloading.api.ClassLoadingHelper;
import org.mule.test.classloading.api.validation.ClassLoadingValidationsProvider;

@Extension(name = "ClassLoading")
@JavaVersionSupport({JAVA_21, JAVA_17})
@Xml(prefix = "classloading")
@ConnectionProviders({CLPoolingConnectionProvider.class, CLCachedConnectionProvider.class, CLNoneConnectionProvider.class})
@Configurations({CLConfiguration.class, CLInvalidConfiguration.class})
@Export(classes = {ClassLoadingHelper.class, ClassLoadingValidationsProvider.class},
    resources = {"META-INF/services/org.mule.runtime.ast.api.validation.ValidationsProvider"})
public class ClassLoadingExtension {

}

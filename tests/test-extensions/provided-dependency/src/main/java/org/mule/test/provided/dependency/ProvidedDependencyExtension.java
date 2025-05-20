/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.provided.dependency;

import static org.mule.sdk.api.meta.JavaVersion.JAVA_17;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_21;

import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.sdk.api.annotation.JavaVersionSupport;
import org.mule.sdk.api.annotation.dsl.xml.Xml;

@Extension(name = ProvidedDependencyExtension.NAME)
@JavaVersionSupport({JAVA_21, JAVA_17})
@Xml(prefix = "provided-dependency")
@ConnectionProviders(ProvidedDependencyConnectionProvider.class)
@Operations(ProvidedDependencyOperations.class)
public class ProvidedDependencyExtension {

  public static final String NAME = "Provided Dependency";
}

/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.customos.internal;

import static org.mule.sdk.api.meta.JavaVersion.JAVA_17;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_21;

import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.sdk.api.annotation.JavaVersionSupport;
import org.mule.test.customos.internal.connection.MyOSConnectionProvider;
import org.mule.test.customos.internal.operation.MyOSOperations;

import java.util.HashMap;
import java.util.Map;

@Extension(name = "MyOS")
@JavaVersionSupport({JAVA_21, JAVA_17})
@Operations(MyOSOperations.class)
@ConnectionProviders(MyOSConnectionProvider.class)
@Xml(prefix = "custom-os")
public class MyOSConnector {

  public static Map<String, TypedValue<String>> VALUES = new HashMap<>();
}

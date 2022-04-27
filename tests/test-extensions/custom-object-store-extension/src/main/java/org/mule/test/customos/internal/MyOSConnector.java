/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.customos.internal;

import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.test.customos.internal.connection.AnotherCustomSdkObjectStoreConnectionProvider;
import org.mule.test.customos.internal.connection.CustomSdkObjectStoreConnectionProvider;
import org.mule.test.customos.internal.connection.MyOSConnectionProvider;
import org.mule.test.customos.internal.operation.MyOSOperations;

import java.util.HashMap;
import java.util.Map;

@Extension(name = "MyOS")
@Operations(MyOSOperations.class)
@ConnectionProviders({MyOSConnectionProvider.class, AnotherCustomSdkObjectStoreConnectionProvider.class})
@org.mule.sdk.api.annotation.connectivity.ConnectionProviders({CustomSdkObjectStoreConnectionProvider.class})
@Xml(prefix = "custom-os")
public class MyOSConnector {

  public static Map<String, TypedValue<String>> VALUES = new HashMap<>();

  public static Map<String, TypedValue<String>> STORAGE = new HashMap<>();
}

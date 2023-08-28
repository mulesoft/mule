/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.customos.internal;

import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.test.customos.internal.connection.MyOSConnectionProvider;
import org.mule.test.customos.internal.operation.MyOSOperations;

import java.util.HashMap;
import java.util.Map;

@Extension(name = "MyOS")
@Operations(MyOSOperations.class)
@ConnectionProviders(MyOSConnectionProvider.class)
@Xml(prefix = "custom-os")
public class MyOSConnector {

  public static Map<String, TypedValue<String>> VALUES = new HashMap<>();
}

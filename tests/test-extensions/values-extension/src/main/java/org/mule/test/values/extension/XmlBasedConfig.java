/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.values.extension;

import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;

@Configuration(name = "xml")
@Operations(XmlBasedOperations.class)
public class XmlBasedConfig {
}

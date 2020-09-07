/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.data.sample.extension;

import org.mule.sdk.api.annotation.Configurations;
import org.mule.sdk.api.annotation.Extension;
import org.mule.sdk.api.annotation.Sources;
import org.mule.sdk.api.annotation.dsl.xml.Xml;
import org.mule.test.data.sample.extension.config.SampleDataConfig;
import org.mule.test.data.sample.extension.source.SimpleSource;

@Extension(name = "Values")
@Configurations(SampleDataConfig.class)
@Sources({SimpleSource.class})
@Xml(namespace = "http://www.mulesoft.org/schema/mule/sample-data", prefix = "sample-data")
public class SampleDataExtension {

  public static final String NULL_VALUE = "<<null>>";
}

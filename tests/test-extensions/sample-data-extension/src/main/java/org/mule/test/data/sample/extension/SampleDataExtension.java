/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.data.sample.extension;

import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.sdk.api.runtime.operation.Result;
import org.mule.test.data.sample.extension.config.SampleDataConfig;
import org.mule.test.data.sample.extension.source.SimpleSource;

@Extension(name = "SampleData")
@Configurations(SampleDataConfig.class)
@Sources({SimpleSource.class})
@Xml(namespace = "http://www.mulesoft.org/schema/mule/sample-data", prefix = "sample-data")
public class SampleDataExtension {

  public static final String NULL_VALUE = "<<null>>";

  public static <T, A> Result<T, A> adaptLegacy(org.mule.runtime.extension.api.runtime.operation.Result<T, A> result) {
    return Result.<T, A>builder()
        .output(result.getOutput())
        .mediaType(result.getMediaType().orElse(null))
        .attributes(result.getAttributes().orElse(null))
        .attributesMediaType(result.getAttributesMediaType().orElse(null))
        .build();
  }
}

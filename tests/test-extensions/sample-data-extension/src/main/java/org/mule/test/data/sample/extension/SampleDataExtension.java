/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.data.sample.extension;

import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.dsl.xml.Xml;
import org.mule.sdk.api.runtime.operation.Result;
import org.mule.test.data.sample.extension.connection.SampleDataConnectionProvider;
import org.mule.test.data.sample.extension.source.AliasedParameterGroupListener;
import org.mule.test.data.sample.extension.source.ComplexParameterGroupListener;
import org.mule.test.data.sample.extension.source.ConfigTestSampleDataListener;
import org.mule.test.data.sample.extension.source.ConnectedTestSampleDataListener;
import org.mule.test.data.sample.extension.source.MuleContextAwareTestSampleDataListener;
import org.mule.test.data.sample.extension.source.ParameterGroupListener;
import org.mule.test.data.sample.extension.source.ShowInDslParameterGroupListener;
import org.mule.test.data.sample.extension.source.SimpleTestSampleDataListener;
import org.mule.test.data.sample.extension.source.SimpleTestSampleDataListenerWithTwoBoundActingParameters;

@Extension(name = SampleDataExtension.EXTENSION_NAME)
@Xml(namespace = "http://www.mulesoft.org/schema/mule/sample-data", prefix = "sample-data")
@ConnectionProviders(SampleDataConnectionProvider.class)
@Operations(SampleDataOperations.class)
@Sources({SimpleTestSampleDataListener.class, ConnectedTestSampleDataListener.class, ConfigTestSampleDataListener.class,
    ParameterGroupListener.class, ShowInDslParameterGroupListener.class, AliasedParameterGroupListener.class,
    ComplexParameterGroupListener.class, MuleContextAwareTestSampleDataListener.class,
    SimpleTestSampleDataListenerWithTwoBoundActingParameters.class
})
public class SampleDataExtension {

  public static final String NULL_VALUE = "<<null>>";
  public static final String EXTENSION_NAME = "SampleData";

  public static <T, A> Result<T, A> adaptLegacy(org.mule.runtime.extension.api.runtime.operation.Result<T, A> result) {
    return Result.<T, A>builder()
        .output(result.getOutput())
        .mediaType(result.getMediaType().orElse(null))
        .attributes(result.getAttributes().orElse(null))
        .attributesMediaType(result.getAttributesMediaType().orElse(null))
        .build();
  }

  @Parameter
  private String prefix;

  public String getPrefix() {
    return prefix;
  }
}

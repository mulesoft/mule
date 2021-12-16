/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.metadata.extension;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.execution.OnError;
import org.mule.runtime.extension.api.annotation.execution.OnSuccess;
import org.mule.runtime.extension.api.annotation.metadata.MetadataScope;
import org.mule.runtime.extension.api.annotation.metadata.fixed.InputJsonType;
import org.mule.runtime.extension.api.annotation.metadata.fixed.InputXmlType;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.source.EmitsResponse;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;
import org.mule.tck.message.StringAttributes;
import org.mule.test.metadata.extension.resolver.JavaOutputStaticTypeResolver;

import java.io.InputStream;

@Alias("custom-static-metadata")
@EmitsResponse
@MetadataScope(outputResolver = JavaOutputStaticTypeResolver.class)
@MediaType(value = "application/java", strict = false)
public class CustomStaticMetadataSource extends Source<Object, StringAttributes> {

  public static String onSuccessResult;
  public static String onErrorResult;

  @Override
  public void onStart(SourceCallback sourceCallback) throws MuleException {
    sourceCallback.handle(Result.builder().output("something").build());
  }

  @OnError
  public void onError(@InputJsonType(schema = "person-schema.json") InputStream person, SourceCallbackContext cc) {
    onErrorResult = IOUtils.toString(person);
  }

  @OnSuccess
  public void onSuccess(@InputXmlType(schema = "order.xsd", qname = "shiporder") InputStream order, SourceCallbackContext cc) {
    onSuccessResult = IOUtils.toString(order);
  }

  @Override
  public void onStop() {

  }
}

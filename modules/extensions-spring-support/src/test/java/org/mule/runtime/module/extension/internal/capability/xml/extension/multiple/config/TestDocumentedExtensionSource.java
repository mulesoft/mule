/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.extension.multiple.config;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.annotation.execution.OnError;
import org.mule.runtime.extension.api.annotation.execution.OnSuccess;
import org.mule.runtime.extension.api.annotation.execution.OnTerminate;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;

/**
 * TestDocumentedExtensionSource javadoc documentation
 */
@MediaType("application/json")
public class TestDocumentedExtensionSource extends Source<String, String> {

  /**
   * Source parameter 1
   */
  @Parameter
  private String source1;

  /**
   * Source Parameter group
   */
  @ParameterGroup(name = "Source group")
  private TestDocumentedParameterGroup group;

  @Override
  public void onStart(SourceCallback<String, String> sourceCallback) throws MuleException {

  }

  @Override
  public void onStop() {

  }

  /**
   * @param responseGroup Callback Doc of responseGroup
   */
  @OnSuccess
  public void onSuccess(@org.mule.sdk.api.annotation.param.ParameterGroup(name = "Response group",
      showInDsl = true) TestDocumentedParameterGroup responseGroup) {

  }

  /**
   * @param errorParam Callback Doc of errorParam
   */
  @OnError
  public void onError(String errorParam) {

  }

  @OnTerminate
  public void doNothing() {}

}

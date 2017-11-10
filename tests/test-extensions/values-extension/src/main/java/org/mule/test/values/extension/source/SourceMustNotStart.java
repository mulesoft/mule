/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.values.extension.source;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.values.OfValues;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.test.values.extension.resolver.TrueFalseValueProvider;

@MediaType(TEXT_PLAIN)
public class SourceMustNotStart extends AbstractSource {

  public static boolean isStarted = false;

  @Override
  public void onStart(SourceCallback<String, String> sourceCallback) throws MuleException {
    isStarted = true;
    super.onStart(sourceCallback);
  }

  @OfValues(TrueFalseValueProvider.class)
  @Parameter
  String hasBeenStarted;

}

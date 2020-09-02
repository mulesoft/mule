/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.marvel.xmen;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;

@MediaType(TEXT_PLAIN)
public class MagnetoMutantSummon extends Source<InputStream, Void> {

  @Override
  public void onStart(SourceCallback<InputStream, Void> sourceCallback) throws MuleException {
    sourceCallback.handle(makeResult());
  }

  private Result<InputStream, Void> makeResult() {
    return Result.<InputStream, Void>builder()
        .output(new ByteArrayInputStream("We are the future. ... You have lived in the shadows of shame and fear for too long!"
            .getBytes()))
        .build();
  }

  @Override
  public void onStop() {
    // Nothing to do
  }

}

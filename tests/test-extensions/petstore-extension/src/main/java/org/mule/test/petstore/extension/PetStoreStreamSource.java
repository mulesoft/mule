/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.petstore.extension;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.DefaultEncoding;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Alias("pet-source-stream")
@MediaType(value = TEXT_PLAIN, strict = false)
public class PetStoreStreamSource extends Source<InputStream, Object> {

  @ParameterGroup(name = "Breeder")
  private ExclusivePetBreeder breeder;

  @org.mule.sdk.api.annotation.param.DefaultEncoding
  private String encoding;

  private int counter = 0;

  @Override
  public void onStart(SourceCallback<InputStream, Object> sourceCallback) throws MuleException {
    SourceCallbackContext context = sourceCallback.createContext();
    context.setCorrelationId(breeder.getBirds());
    sourceCallback.handle(Result.<InputStream, Object>builder().output(new ByteArrayInputStream(encoding.getBytes())).build(),
                          context);
  }

  @Override
  public void onStop() {}
}

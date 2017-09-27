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

@Alias("pet-source")
@MediaType(TEXT_PLAIN)
public class PetStoreSource extends Source<String, Object> {

  @ParameterGroup(name = "Breeder")
  private ExclusivePetBreeder breeder;

  @DefaultEncoding
  private String encoding;

  @Override
  public void onStart(SourceCallback<String, Object> sourceCallback) throws MuleException {
    sourceCallback.handle(Result.<String, Object>builder().output(encoding).build());
  }

  @Override
  public void onStop() {}
}

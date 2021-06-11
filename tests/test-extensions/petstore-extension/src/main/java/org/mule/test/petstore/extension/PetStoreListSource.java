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
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.sdk.api.runtime.operation.Result;
import org.mule.sdk.api.runtime.source.Source;
import org.mule.sdk.api.runtime.source.SourceCallback;
import org.mule.sdk.api.runtime.source.SourceCallbackContext;

import java.util.LinkedList;
import java.util.List;

@Alias("pet-source-list")
@MediaType(value = TEXT_PLAIN, strict = false)
public class PetStoreListSource extends Source<List<Result<String, Object>>, Object> {

  @org.mule.sdk.api.annotation.param.ParameterGroup(name = "Breeder")
  private ExclusivePetBreeder breeder;

  @Override
  public void onStart(SourceCallback<List<Result<String, Object>>, Object> sourceCallback) throws MuleException {
    SourceCallbackContext context = sourceCallback.createContext();
    context.setCorrelationId(breeder.getBirds());
    List<Result<String, Object>> listResult = new LinkedList<>();
    listResult.add(Result.<String, Object>builder().output("cat").build());
    listResult.add(Result.<String, Object>builder().output("dog").build());
    listResult.add(Result.<String, Object>builder().output("parrot").build());
    sourceCallback.handle(Result.<List<Result<String, Object>>, Object>builder().output(listResult).build(),
                          context);
  }

  @Override
  public void onStop() {}
}

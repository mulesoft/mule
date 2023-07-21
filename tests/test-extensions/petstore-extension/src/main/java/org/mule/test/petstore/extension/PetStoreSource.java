/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.petstore.extension;

import static java.util.Collections.singletonMap;
import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.DefaultEncoding;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;
import org.mule.sdk.api.annotation.param.RuntimeVersion;

@Alias("pet-source")
@MediaType(TEXT_PLAIN)
public class PetStoreSource extends Source<String, Object> {

  @ParameterGroup(name = "Breeder")
  private ExclusivePetBreeder breeder;

  @DefaultEncoding
  private String encoding;

  @RuntimeVersion
  private MuleVersion muleVersion;

  private int counter = 0;

  @Override
  public void onStart(SourceCallback<String, Object> sourceCallback) throws MuleException {
    SourceCallbackContext context = sourceCallback.createContext();
    context.setCorrelationId(breeder.getBirds());

    Result result = Result.<String, Object>builder()
        .output(encoding)
        .attributes(singletonMap("muleRuntime", muleVersion))
        .build();

    sourceCallback.handle(result, context);
  }

  @Override
  public void onStop() {}
}

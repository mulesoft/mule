/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension;

import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.stereotype.AllowedStereotypes;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.process.Chain;
import org.mule.test.heisenberg.extension.stereotypes.DrugKillingStereotype;
import org.mule.test.heisenberg.extension.stereotypes.KillingStereotype;

import java.util.Map;

public class HeisenbergScopes {

  @Throws(HeisenbergErrorTyperProvider.class)
  public void killMany(@AllowedStereotypes({KillingStereotype.class, DrugKillingStereotype.class}) Chain killOperations,
                       CompletionCallback<String, Void> callback, String reason)
      throws Exception {
    // TODO
    callback.success(Result.<String, Void>builder().output(reason).build());
  }

  public void executeAnything(Chain chain, CompletionCallback<Void, Void> cb) {
    chain.onSuccess(cb::success).process();
  }

  public void payloadModifier(Chain chain, CompletionCallback<Void, Void> cb,
                              Object payload, Map<String, String> attributes) {
    chain.onSuccess(cb::success).process(payload, attributes);
  }

  public void exceptionOnCallbacks(Chain processors, CompletionCallback<Void, Void> callback) {
    processors
        .onSuccess(result -> {
          throw new IllegalArgumentException("ON_SUCCESS_EXCEPTION");
        })
        .onError((error, previous) -> {
          throw new IllegalArgumentException("ON_ERROR_EXCEPTION");
        })
        .process();
  }

  public void alwaysFailsWrapper(Chain processors, CompletionCallback<Void, Void> callback) {
    processors
        .onSuccess(result -> callback.error(new IllegalArgumentException("ON_SUCCESS_ERROR")))
        .onError((error, previous) -> callback.error(new IllegalArgumentException("ON_ERROR_ERROR")))
        .process();
  }

  public void neverFailsWrapper(@Optional Chain optionalProcessors,
                                CompletionCallback<String, Object> callback) {
    if (optionalProcessors == null) {
      callback.success(Result.<String, Object>builder().output("EMPTY").build());
    } else {
      optionalProcessors
          .onSuccess(result -> callback.success(result.copy().output("SUCCESS").attributes(result).build()))

          .onError((error, previous) -> callback.success(previous.copy().output("ERROR").attributes(error).build()))
          .process();
    }
  }

}

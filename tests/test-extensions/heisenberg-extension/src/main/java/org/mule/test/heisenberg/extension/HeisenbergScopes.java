/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension;

import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.stereotype.AllowedStereotypes;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.process.Chain;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.test.heisenberg.extension.stereotypes.DrugKillingStereotype;
import org.mule.test.heisenberg.extension.stereotypes.KillingStereotype;

import java.util.Map;

public class HeisenbergScopes implements Initialisable {

  private int initialiasedCounter = 0;

  @Override
  public void initialise() throws InitialisationException {
    initialiasedCounter++;
  }

  public int getCounter() {
    return initialiasedCounter;
  }

  @Throws(HeisenbergErrorTyperProvider.class)
  public void killMany(@AllowedStereotypes({KillingStereotype.class, DrugKillingStereotype.class}) Chain killOperations,
                       CompletionCallback<String, Void> callback, String reason)
      throws Exception {
    //TODO MULE-13440
  }

  @OutputResolver(output = HeisenbergOutputResolver.class)
  public void getChain(Chain chain, CompletionCallback<Chain, Void> cb) {
    cb.success(Result.<Chain, Void>builder().output(chain).build());
  }

  public void executeAnything(Chain chain, CompletionCallback<Void, Void> cb) {
    chain.process(cb::success, (t, e) -> cb.error(t));
  }

  public void payloadModifier(Chain chain, CompletionCallback<Void, Void> cb,
                              Object payload, Map<String, String> attributes) {
    chain.process(payload, attributes, cb::success, (t, e) -> cb.error(t));
  }

  public void exceptionOnCallbacks(Chain processors, CompletionCallback<Void, Void> callback) {
    processors
        .process(result -> {
          throw new IllegalArgumentException("ON_SUCCESS_EXCEPTION");
        }, (error, previous) -> {
          throw new IllegalArgumentException("ON_ERROR_EXCEPTION");
        });
  }

  public void alwaysFailsWrapper(Chain processors, CompletionCallback<Void, Void> callback) {
    processors.process(result -> callback.error(new IllegalArgumentException("ON_SUCCESS_ERROR")),
                       (error, previous) -> callback.error(new IllegalArgumentException("ON_ERROR_ERROR")));
  }

  public void neverFailsWrapper(@Optional Chain optionalProcessors,
                                CompletionCallback<String, Object> callback) {
    if (optionalProcessors == null) {
      callback.success(Result.<String, Object>builder().output("EMPTY").build());
    } else {
      optionalProcessors.process(result -> callback.success(result.copy().output("SUCCESS").attributes(result).build()),
                                 (error, previous) -> callback
                                     .success(previous.copy().output("ERROR").attributes(error).build()));
    }
  }

}

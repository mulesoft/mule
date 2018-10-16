/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.util.Preconditions;
import org.mule.runtime.extension.api.annotation.deprecated.Deprecated;
import org.mule.runtime.extension.api.annotation.dsl.xml.ParameterDsl;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.stereotype.AllowedStereotypes;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.extension.api.runtime.route.Chain;
import org.mule.runtime.extension.api.stereotype.ValidatorStereotype;

import java.util.Map;

public class HeisenbergScopes implements Initialisable {

  private int initialiasedCounter = 0;

  @Parameter
  @Optional(defaultValue = "0")
  private int fieldParam;

  @Override
  public void initialise() throws InitialisationException {
    initialiasedCounter++;
  }

  public int getCounter() {
    return initialiasedCounter;
  }

  public void getChain(@AllowedStereotypes(ValidatorStereotype.class) Chain chain, CompletionCallback<Chain, Void> cb) {
    cb.success(Result.<Chain, Void>builder().output(chain).build());
  }

  @Deprecated(message = "All usages of this scope are covered by the payload-modifier scope.")
  public void executeAnything(Chain chain, CompletionCallback<Void, Void> cb) {
    chain.process(cb::success, (t, e) -> cb.error(t));
  }

  public void payloadModifier(Chain chain,
                              CompletionCallback<Object, Object> cb,
                              Object payload,
                              @ParameterDsl(allowInlineDefinition = false) Map<String, String> attributes) {
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

  @MediaType(TEXT_PLAIN)
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

  public int scopeField(int expected, int newValue) {
    Preconditions.checkArgument(expected == fieldParam, "Expected " + expected + " but was " + fieldParam);
    fieldParam = newValue;
    return fieldParam;
  }

}

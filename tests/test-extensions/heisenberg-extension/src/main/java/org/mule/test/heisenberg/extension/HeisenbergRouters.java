/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toMap;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.deprecated.Deprecated;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.process.RouterCompletionCallback;
import org.mule.runtime.extension.api.runtime.process.VoidCompletionCallback;
import org.mule.runtime.extension.api.runtime.route.Chain;
import org.mule.test.heisenberg.extension.model.Attribute;
import org.mule.test.heisenberg.extension.route.AfterCall;
import org.mule.test.heisenberg.extension.route.BeforeCall;
import org.mule.test.heisenberg.extension.route.DrugKillingRoute;
import org.mule.test.heisenberg.extension.route.KillingRoute;
import org.mule.test.heisenberg.extension.route.OtherwiseRoute;
import org.mule.test.heisenberg.extension.route.WhenRoute;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class HeisenbergRouters implements Initialisable, Startable, Stoppable, Disposable {

  public static final String NOT_INITIALISED = "not_initialised";
  private String state = NOT_INITIALISED;

  public void voidRouter(WhenRoute when, VoidCompletionCallback callback) {
    when.getChain()
        .process(state, null, r -> callback.success(), (e, r) -> callback.error(e));
  }

  @Parameter
  @Optional(defaultValue = "0")
  private int fieldParam;

  public void concurrentRouteExecutor(WhenRoute when, RouterCompletionCallback callback) {
    Consumer<Chain> processor = (chain) -> {
      final Latch latch = new Latch();
      chain.process((result -> latch.release()), (error, result) -> latch.release());
      try {
        latch.await(10000, MILLISECONDS);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    };

    Thread first = new Thread(() -> processor.accept(when.getChain()));
    Thread second = new Thread(() -> processor.accept(when.getChain()));
    first.start();
    second.start();
    try {
      first.join();
      second.join();
    } catch (Exception e) {
      callback.error(e);
    }
    callback.success(Result.builder().output("SUCCESS").build());
  }

  @Deprecated(message = "Simple routers are too simple, we will no longer suport them.")
  public void simpleRouter(WhenRoute when, RouterCompletionCallback callback) {
    when.getChain()
        .process(callback::success,
                 (e, r) -> callback.error(e));
  }

  public void twoRoutesRouter(String processorName,
                              WhenRoute when,
                              @Optional OtherwiseRoute other,
                              RouterCompletionCallback callback) {
    if (when.shouldExecute()) {
      when.getChain().process(Result.builder()
          .output(processorName)
          .attributes(when.getMessage()).build(),
                              callback::success, (e, r) -> callback.error(e));
    } else if (other != null && other.shouldExecute()) {
      other.getChain().process(processorName, null, callback::success, (e, r) -> callback.error(e));
    } else {
      callback.error(new IllegalArgumentException("No route executed"));
    }
  }

  public void stereotypedRoutes(KillingRoute killingRoute,
                                @Optional DrugKillingRoute drugKillingRoute,
                                RouterCompletionCallback callback) {
    killingRoute.getChain().process(result -> {
      if (drugKillingRoute != null) {
        drugKillingRoute.getChain().process(result, callback::success, (e, r) -> callback.error(e));
      } else {
        callback.success(result);
      }
    }, (e, r) -> callback.error(e));
  }

  @Summary("Allows to take actions over the event before and after the execution of a processor")
  public void spy(String processor,
                  @Optional @NullSafe @Expression(NOT_SUPPORTED) List<Attribute> withAttributes,
                  @Optional BeforeCall beforeCallAssertions,
                  @Optional AfterCall afterCallAssertions,
                  RouterCompletionCallback callback) {
    Map<String, Object> attr = withAttributes.stream().collect(toMap(Attribute::getName, r -> r));

    if (beforeCallAssertions == null && afterCallAssertions == null) {
      callback.success(Result.builder().build());
    }

    if (beforeCallAssertions != null) {
      beforeCallAssertions.getChain()
          // Control de payload/attributes of the chain execution
          .process(attr, null,
                   success -> {
                     // execute the element being spyied
                     System.out.println(processor);
                     // and then execute afterAssertions using the result of that MP,
                     // in this case we just pipe the previous "success"
                     if (afterCallAssertions != null) {
                       afterCallAssertions.getChain()
                           // Complete the execution of the router
                           .process(success, callback::success, (t, e) -> callback.error(t));
                     } else {
                       callback.success(success);
                     }
                   },
                   (error, lastResult) -> callback.error(error));
    } else if (afterCallAssertions != null) {
      afterCallAssertions.getChain()
          .process(attr, null, callback::success, (t, e) -> callback.error(t));
    } else {
      callback.success(Result.builder().build());
    }
  }

  @Override
  public void initialise() throws InitialisationException {
    checkState(NOT_INITIALISED);
    state = Initialisable.PHASE_NAME;
  }

  @Override
  public void start() throws MuleException {
    checkState(Initialisable.PHASE_NAME);
    state = Startable.PHASE_NAME;
  }

  @Override
  public void stop() throws MuleException {
    checkState(Startable.PHASE_NAME);
    state = Stoppable.PHASE_NAME;
  }

  @Override
  public void dispose() {
    try {
      checkState(Stoppable.PHASE_NAME);
    } catch (InitialisationException e) {
      throw new RuntimeException(e);
    }
    state = Disposable.PHASE_NAME;
  }

  private void checkState(String expected) throws InitialisationException {
    if (!state.equals(expected)) {
      throw new InitialisationException(
                                        createStaticMessage("Invalid state: expected %s but as %s", expected, state), this);
    }
  }

  public int routerField(int expected, int newValue) {
    checkArgument(expected == fieldParam, "Expected " + expected + " but was " + fieldParam);
    fieldParam = newValue;
    return fieldParam;
  }

}

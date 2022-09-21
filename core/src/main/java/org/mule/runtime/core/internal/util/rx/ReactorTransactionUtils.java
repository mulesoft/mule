/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.rx;

import static java.util.Collections.emptyList;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Function;

import reactor.util.context.Context;
import reactor.util.context.ContextView;

/**
 * Utils class to allow transactional behavior in reactor.
 *
 * @since 4.5, 4.4.1, 4.3.1
 */
public final class ReactorTransactionUtils {

  private ReactorTransactionUtils() {
    // Nothing to do
  }

  public static final String TX_SCOPES_KEY = "mule.tx.activeTransactionsInReactorChain";

  public static boolean isTxActiveByContext(ContextView ctx) {
    return ctx != null && ctx.<Deque<String>>getOrEmpty(TX_SCOPES_KEY).map(txScopes -> !txScopes.isEmpty()).orElse(false);
  }

  /**
   * Cleanup the state set by {@link #pushTxToSubscriberContext(String)}.
   */
  public static Function<Context, Context> popTxFromSubscriberContext() {
    return context -> {
      Deque<String> currentTxChains = new ArrayDeque<>(context.getOrDefault(TX_SCOPES_KEY, emptyList()));
      currentTxChains.pop();
      return context.put(TX_SCOPES_KEY, currentTxChains);
    };
  }

  /**
   * Force the upstream publisher to behave as if a transaction were active, effectively avoiding thread switches.
   */
  public static Function<Context, Context> pushTxToSubscriberContext(String location) {
    return context -> {
      Deque<String> currentTxChains = new ArrayDeque<>(context.getOrDefault(TX_SCOPES_KEY, emptyList()));
      currentTxChains.push(location);
      return context.put(TX_SCOPES_KEY, currentTxChains);
    };
  }
}


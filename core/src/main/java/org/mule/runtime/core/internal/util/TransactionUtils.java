/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import reactor.util.context.Context;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Function;

import static java.util.Collections.emptyList;

public class TransactionUtils {

  public static final String TX_SCOPES_KEY = "mule.tx.activeTransactionsInReactorChain";

  public static boolean isTxActive(Context ctx) {
    return ctx.<Deque<String>>getOrEmpty(TX_SCOPES_KEY).map(txScopes -> !txScopes.isEmpty()).orElse(false);
  }

  /**
   * Cleanup the state set by {@link #pushTxToSubscriberContext(String)}.
   *
   * @since 4.3
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
   *
   * @since 4.3
   */
  public static Function<Context, Context> pushTxToSubscriberContext(String location) {
    return context -> {
      Deque<String> currentTxChains = new ArrayDeque<>(context.getOrDefault(TX_SCOPES_KEY, emptyList()));
      currentTxChains.push(location);
      return context.put(TX_SCOPES_KEY, currentTxChains);
    };
  }
}


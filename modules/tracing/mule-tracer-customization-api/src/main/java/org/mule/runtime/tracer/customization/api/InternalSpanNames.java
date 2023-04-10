/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.customization.api;

/**
 * Names for spans that are created internally. The behavior for this spans will not be configurable.
 *
 * @since 4.6.0
 */
public class InternalSpanNames {

  public static final String TRY_SCOPE_INNER_CHAIN_SPAN_NAME = "try-scope-inner-chain";
  public static final String EXECUTE_NEXT_SPAN_NAME = "execute-next";
  public static final String POLICY_NEXT_ACTION_SPAN_NAME = "policy-next-action";
  public static final String POLICY_CHAIN_SPAN_NAME = "mule:policy-chain";
  public static final String ASYNC_INNER_CHAIN_SPAN_NAME = "async-inner-chain";
  public static final String CACHE_CHAIN_SPAN_NAME = "mule:cache-chain";
  public static final String MESSAGE_PROCESSORS_SPAN_NAME = "message:processor";
}

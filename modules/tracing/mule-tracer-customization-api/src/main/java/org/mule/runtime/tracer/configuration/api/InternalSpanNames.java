/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.configuration.api;

/**
 * Names for spans that are created internally. The behavior for this spans will not be configurable.
 *
 * @since 4.6.0
 */
public class InternalSpanNames {

  public static final String EXECUTE_NEXT_COMPONENT_NAME = "execute-next";
  public static final String MULE_POLICY_NEXT_ACTION_EXPORT_INFO_KEY = "policy-next-action";
  public static final String MULE_POLICY_CHAIN_INITIAL_EXPORT_INFO_KEY = "mule:policy-chain";
  public static final String ASYNC_INNER_CHAIN = "async-inner-chain";
  public static final String MULE_CACHE_CHAIN = "mule:cache-chain";
}

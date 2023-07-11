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
 * @since 4.5.0
 */
public class InternalSpanNames {

  private InternalSpanNames() {}

  public static final String TRY_SCOPE_INNER_CHAIN_SPAN_NAME = "try-scope-inner-chain";
  public static final String EXECUTE_NEXT_SPAN_NAME = "http-policy:execute-next";
  public static final String POLICY_NEXT_ACTION_SPAN_NAME = "mule:policy-next-action";
  public static final String POLICY_SOURCE_SPAN_NAME = "http-policy:source";
  public static final String POLICY_OPERATION_SPAN_NAME = "http-policy:operation";
  public static final String ASYNC_INNER_CHAIN_SPAN_NAME = "mule:async-inner-chain";
  public static final String CACHE_CHAIN_SPAN_NAME = "mule:cache-chain";
  public static final String MESSAGE_PROCESSORS_SPAN_NAME = "message:processor";
  public static final String HTTP_REQUEST_SPAN_NAME = "http:request";
  public static final String MULE_FLOW_SPAN_NAME = "mule:flow";
  public static final String MULE_SUB_FLOW_SPAN_NAME = "mule:subflow";
  public static final String GET_CONNECTION_SPAN_NAME = "mule:get-connection";
  public static final String PARAMETERS_RESOLUTION_SPAN_NAME = "mule:parameters-resolution";
  public static final String OPERATION_EXECUTION_SPAN_NAME = "mule:operation-execution";
  public static final String ON_ERROR_PROPAGATE_SPAN_NAME = "mule:on-error-propagate";
  public static final String ON_ERROR_CONTINUE_SPAN_NAME = "mule:on-error-continue";
  public static final String VALUE_RESOLUTION_SPAN_NAME = "mule:value-resolution";


}

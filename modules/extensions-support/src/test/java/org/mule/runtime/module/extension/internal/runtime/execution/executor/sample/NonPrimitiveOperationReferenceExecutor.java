/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution.executor.sample;

import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.streaming.StreamingHelper;
import org.mule.runtime.module.extension.internal.runtime.execution.executor.MethodExecutor;
import org.mule.runtime.module.extension.internal.runtime.execution.executor.MethodExecutorGeneratorTestCase;
import org.mule.runtime.module.extension.internal.runtime.resolver.ArgumentResolver;

import java.util.Map;

public class NonPrimitiveOperationReferenceExecutor implements MethodExecutor {

  private final MethodExecutorGeneratorTestCase __targetInstance;
  private final ArgumentResolver<MethodExecutorGeneratorTestCase> configResolver;
  private final ArgumentResolver<String> param1Resolver;
  private final ArgumentResolver<Map<String, Object>> mapResolver;
  private final ArgumentResolver<StreamingHelper> streamingHelperResolver;

  public Object execute(ExecutionContext executionContext) throws Exception {
    return this.__targetInstance.noPrimitivesOperation(this.configResolver.resolve(executionContext),
                                                       this.param1Resolver.resolve(executionContext),
                                                       this.mapResolver.resolve(executionContext),
                                                       this.streamingHelperResolver.resolve(executionContext));
  }

  public NonPrimitiveOperationReferenceExecutor(MethodExecutorGeneratorTestCase __targetInstance,
                                                ArgumentResolver configResolver,
                                                ArgumentResolver param1Resolver,
                                                ArgumentResolver mapResolver,
                                                ArgumentResolver streamingHelperResolver) {
    this.__targetInstance = __targetInstance;
    this.configResolver = configResolver;
    this.param1Resolver = param1Resolver;
    this.mapResolver = mapResolver;
    this.streamingHelperResolver = streamingHelperResolver;
  }
}

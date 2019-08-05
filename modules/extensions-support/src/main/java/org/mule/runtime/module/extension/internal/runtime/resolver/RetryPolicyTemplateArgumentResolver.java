/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;

import java.util.function.Supplier;

public class RetryPolicyTemplateArgumentResolver implements ArgumentResolver<RetryPolicyTemplate> {

  @Override
  public Supplier<RetryPolicyTemplate> resolve(ExecutionContext executionContext) {
    ExecutionContextAdapter<OperationModel> ctx = (ExecutionContextAdapter<OperationModel>) executionContext;
    return () -> ctx.getRetryPolicyTemplate().get();
  }
}

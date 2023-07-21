/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;

/**
 * A {@link ArgumentResolver} that returns the {@link RetryPolicyTemplate} effective for the operation
 *
 * @since 4.2.2 - 4.3.0
 */
public class RetryPolicyTemplateArgumentResolver implements ArgumentResolver<RetryPolicyTemplate> {

  @Override
  public RetryPolicyTemplate resolve(ExecutionContext executionContext) {
    ExecutionContextAdapter<OperationModel> ctx = (ExecutionContextAdapter<OperationModel>) executionContext;
    return ctx.getRetryPolicyTemplate().get();
  }
}

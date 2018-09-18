/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
/*
 * Decompiled with CFR 0_132.
 * 
 * Could not load the following classes:
 *  org.mule.runtime.extension.api.runtime.operation.ExecutionContext
 *  org.mule.runtime.module.extension.internal.runtime.execution.ByteBuddyWrappedMethodComponentExecutor
 *  org.mule.runtime.module.extension.internal.runtime.execution.ByteBuddyWrappedMethodComponentExecutor$MethodComponentExecutorWrapper
 *  org.mule.test.heisenberg.extension.HeisenbergScopes
 *  org.mule.test.heisenberg.extension.HeisenbergScopes$getCounter$746721375$MethodComponentExecutorWrapper
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.execution.ByteBuddyWrappedMethodComponentExecutor;
import org.mule.test.heisenberg.extension.HeisenbergScopes;

public class GetCounter
    implements ByteBuddyWrappedMethodComponentExecutor.MethodComponentExecutorWrapper {

  private final HeisenbergScopes componentInstance;

  public Object execute(ExecutionContext executionContext) {
    return Integer.valueOf(this.componentInstance.getCounter());
  }

  public GetCounter(HeisenbergScopes heisenbergScopes) {
    this.componentInstance = heisenbergScopes;
  }
}

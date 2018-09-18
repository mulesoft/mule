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
 *  org.mule.runtime.module.extension.internal.runtime.resolver.ArgumentResolver
 *  org.mule.test.heisenberg.extension.HeisenbergExtension
 *  org.mule.test.heisenberg.extension.HeisenbergOperations
 *  org.mule.test.heisenberg.extension.HeisenbergOperations$sayMyName$32923475$MethodComponentExecutorWrapper
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import java.util.function.Supplier;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.execution.ByteBuddyWrappedMethodComponentExecutor;
import org.mule.runtime.module.extension.internal.runtime.resolver.ArgumentResolver;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.heisenberg.extension.HeisenbergOperations;

public class SayMyName
    implements ByteBuddyWrappedMethodComponentExecutor.MethodComponentExecutorWrapper {

  private final HeisenbergOperations componentInstance;
  private final ArgumentResolver<HeisenbergExtension> configResolver;

  public Object execute(ExecutionContext executionContext) {
    return this.componentInstance.sayMyName(this.configResolver.resolve(executionContext).get());
  }

  public SayMyName(HeisenbergOperations heisenbergOperations, ArgumentResolver<HeisenbergExtension> argumentResolver) {
    this.componentInstance = heisenbergOperations;
    this.configResolver = argumentResolver;
  }
}

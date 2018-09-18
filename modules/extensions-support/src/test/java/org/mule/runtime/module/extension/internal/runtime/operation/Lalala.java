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
 *  org.mule.runtime.module.extension.internal.runtime.operation.ReflectiveMethodOperationExecutorTestCase
 *  org.mule.runtime.module.extension.internal.runtime.operation.ReflectiveMethodOperationExecutorTestCase$PrimitiveTypesTestOperations
 *  org.mule.runtime.module.extension.internal.runtime.operation.ReflectiveMethodOperationExecutorTestCase$PrimitiveTypesTestOperations$allCombined$1490799076$MethodComponentExecutorWrapper
 *  org.mule.runtime.module.extension.internal.runtime.resolver.ArgumentResolver
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.execution.ByteBuddyWrappedMethodComponentExecutor;
import org.mule.runtime.module.extension.internal.runtime.operation.ReflectiveMethodOperationExecutorTestCase;
import org.mule.runtime.module.extension.internal.runtime.resolver.ArgumentResolver;

public class Lalala
    implements ByteBuddyWrappedMethodComponentExecutor.MethodComponentExecutorWrapper {

  private final ReflectiveMethodOperationExecutorTestCase.PrimitiveTypesTestOperations componentInstance;
  private final ArgumentResolver<Character> charValueResolver;
  private final ArgumentResolver<Byte> byteValueResolver;
  private final ArgumentResolver<Short> shortValueResolver;
  private final ArgumentResolver<Integer> intValueResolver;
  private final ArgumentResolver<Long> longValueResolver;
  private final ArgumentResolver<Float> floatValueResolver;
  private final ArgumentResolver<Double> doubleValueResolver;
  private final ArgumentResolver<Boolean> booleanValueResolver;

  /*
   * Exception decompiling
   */
  public Object execute(ExecutionContext var1) {
    componentInstance.allCombined(charValueResolver.resolve(var1).get(), byteValueResolver.resolve(var1).get(),
                                  shortValueResolver.resolve(var1).get(),
                                  intValueResolver.resolve(var1).get(), longValueResolver.resolve(var1).get(),
                                  floatValueResolver.resolve(var1).get(),
                                  doubleValueResolver.resolve(var1).get(), booleanValueResolver.resolve(var1).get());
    return null;
  }

  public Lalala(ReflectiveMethodOperationExecutorTestCase.PrimitiveTypesTestOperations primitiveTypesTestOperations,
                ArgumentResolver<Character> argumentResolver, ArgumentResolver<Byte> argumentResolver2,
                ArgumentResolver<Short> argumentResolver3, ArgumentResolver<Integer> argumentResolver4,
                ArgumentResolver<Long> argumentResolver5, ArgumentResolver<Float> argumentResolver6,
                ArgumentResolver<Double> argumentResolver7, ArgumentResolver<Boolean> argumentResolver8) {
    this.componentInstance = primitiveTypesTestOperations;
    this.charValueResolver = argumentResolver;
    this.byteValueResolver = argumentResolver2;
    this.shortValueResolver = argumentResolver3;
    this.intValueResolver = argumentResolver4;
    this.longValueResolver = argumentResolver5;
    this.floatValueResolver = argumentResolver6;
    this.doubleValueResolver = argumentResolver7;
    this.booleanValueResolver = argumentResolver8;
  }
}

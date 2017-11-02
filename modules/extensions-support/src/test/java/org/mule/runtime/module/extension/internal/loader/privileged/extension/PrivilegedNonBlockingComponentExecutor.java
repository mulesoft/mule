/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.privileged.extension;

import static reactor.core.publisher.Mono.delay;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.extension.api.runtime.operation.ComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;

import java.time.Duration;

import org.reactivestreams.Publisher;

public class PrivilegedNonBlockingComponentExecutor implements ComponentExecutor<OperationModel> {

  public static final String OUTPUT = "Super Special Non Blocking";

  @Override
  public Publisher<Object> execute(ExecutionContext<OperationModel> executionContext) {
    return delay(Duration.ofSeconds(2)).map(l -> OUTPUT);
  }
}

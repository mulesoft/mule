/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client;

import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.extension.api.client.OperationParameterizer;
import org.mule.runtime.extension.api.component.ComponentParameterization;

import java.util.function.Consumer;

class DefaultOperationParameterizer implements OperationParameterizer {

  private String configRef;
  private ComponentParameterization<OperationModel> parameterization;


  @Override
  public OperationParameterizer withConfigRef(String configurationName) {
    return null;
  }

  @Override
  public OperationParameterizer parameters(ComponentParameterization<OperationModel> parameterization) {
    return null;
  }

  @Override
  public OperationParameterizer withSimpleReconnection(int frequency, int count) {
    return null;
  }

  @Override
  public OperationParameterizer reconnectingForever(int frequency) {
    return null;
  }

  @Override
  public OperationParameterizer withInMemoryRepeatableStream(Consumer<InMemoryRepeatableStreamConfigurer> configurer) {
    return null;
  }

  @Override
  public OperationParameterizer withInMemoryRepeatableIterable(Consumer<InMemoryRepeatableIterableConfigurer> configurer) {
    return null;
  }

  @Override
  public OperationParameterizer withFileStoreRepeatableStream(Consumer<FileStoreRepeatableStreamConfigurer> configurer) {
    return null;
  }

  @Override
  public OperationParameterizer withNonRepeatableStreams() {
    return null;
  }

  String getConfigRef() {
    return configRef;
  }
}

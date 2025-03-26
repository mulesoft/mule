/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.factories;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.exception.EnrichedErrorMapping;
import org.mule.runtime.dsl.api.component.ObjectFactory;
import org.mule.runtime.extension.internal.processor.ModuleOperationMessageProcessor;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

public class ModuleOperationMessageProcessorFactoryBean extends AbstractComponent
    implements ObjectFactory<ModuleOperationMessageProcessor> {

  private Map<String, Object> parameters = emptyMap();

  private List<EnrichedErrorMapping> errorMappings = emptyList();

  private List<Processor> processors;

  private ExtensionModel extensionModel;
  private OperationModel operationModel;

  @Inject
  protected MuleContext muleContext;

  @Inject
  private ExtensionManager extensionManager;

  @Inject
  protected ConfigurationComponentLocator locator;

  @Override
  public ModuleOperationMessageProcessor getObject() throws Exception {
    final ModuleOperationMessageProcessor messageProcessorChain =
        new ModuleOperationMessageProcessor(parameters, errorMappings, extensionManager, extensionModel, operationModel);

    messageProcessorChain.setAnnotations(getAnnotations());
    messageProcessorChain.setMessageProcessors(processors == null ? emptyList() : processors);
    return messageProcessorChain;
  }

  public void setParameters(Map<String, Object> parameters) {
    this.parameters = parameters;
  }

  public void setExtensionModel(ExtensionModel extensionModel) {
    this.extensionModel = extensionModel;
  }

  public void setOperationModel(OperationModel operationModel) {
    this.operationModel = operationModel;
  }

  public void setMessageProcessors(List<Processor> processors) {
    this.processors = processors;
  }

  public void setErrorMappings(List<EnrichedErrorMapping> errorMappings) {
    this.errorMappings = errorMappings;
  }
}

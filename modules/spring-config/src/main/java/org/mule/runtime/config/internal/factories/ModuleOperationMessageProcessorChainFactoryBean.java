/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.factories;

import static java.lang.String.format;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.getProcessingStrategy;
import static org.mule.runtime.core.privileged.processor.chain.DefaultMessageProcessorChainBuilder.newLazyProcessorChainBuilder;

import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.util.IdempotentExtensionWalker;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.processor.chain.ModuleOperationMessageProcessorChainBuilder;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChainBuilder;
import org.mule.runtime.core.privileged.processor.objectfactory.MessageProcessorChainObjectFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

public class ModuleOperationMessageProcessorChainFactoryBean extends MessageProcessorChainObjectFactory {

  private Map<String, String> properties = new HashMap<>();
  private Map<String, String> parameters = new HashMap<>();
  private String moduleName;
  private String moduleOperation;
  @Inject
  private ExtensionManager extensionManager;

  @Inject
  protected ConfigurationComponentLocator locator;

  @Override
  public MessageProcessorChain doGetObject() throws Exception {
    MessageProcessorChainBuilder builder = getBuilderInstance();
    for (Object processor : processors) {
      if (processor instanceof Processor) {
        builder.chain((Processor) processor);
      } else {
        throw new IllegalArgumentException(format("MessageProcessorBuilder should only have MessageProcessor's or MessageProcessorBuilder's configured. Found a %s",
                                                  processor.getClass().getName()));
      }
    }
    final MessageProcessorChain messageProcessorChain =
        newLazyProcessorChainBuilder((ModuleOperationMessageProcessorChainBuilder) builder,
                                     muleContext,
                                     () -> getProcessingStrategy(locator, getRootContainerLocation()).orElse(null));
    messageProcessorChain.setAnnotations(getAnnotations());
    messageProcessorChain.setMuleContext(muleContext);
    return messageProcessorChain;
  }

  @Override
  protected MessageProcessorChainBuilder getBuilderInstance() {
    final ExtensionModel extensionModel = getExtensionModelOrFail();
    return new ModuleOperationMessageProcessorChainBuilder(properties, parameters, extensionModel,
                                                           getOperationModelOrFail(extensionModel),
                                                           muleContext.getExpressionManager());
  }

  private ExtensionModel getExtensionModelOrFail() {
    return extensionManager.getExtensions().stream()
        .filter(em -> em.getXmlDslModel().getPrefix().equals(moduleName))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(format("Could not find any extension under the name of [%s]",
                                                               moduleName)));
  }

  private OperationModel getOperationModelOrFail(ExtensionModel extensionModel) {
    OperationSeeker operationSeeker = new OperationSeeker();
    operationSeeker.walk(extensionModel);
    if (!operationSeeker.operationModel.isPresent()) {
      throw new IllegalArgumentException(format("Could not find any operation under the name of [%s] for the extension [%s]",
                                                moduleOperation, moduleName));
    }
    return operationSeeker.operationModel.get();
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }

  public void setParameters(Map<String, String> parameters) {
    this.parameters = parameters;
  }

  public void setModuleName(String moduleName) {
    this.moduleName = moduleName;
  }

  public void setModuleOperation(String moduleOperation) {
    this.moduleOperation = moduleOperation;
  }

  /**
   * Internal class used only as a helper to find the only occurrence of an operation under the same name.
   */
  private class OperationSeeker extends IdempotentExtensionWalker {

    Optional<OperationModel> operationModel = Optional.empty();

    @Override
    protected void onOperation(OperationModel operationModel) {
      if (operationModel.getName().equals(moduleOperation)) {
        this.operationModel = Optional.of(operationModel);
        stop();
      }
    }
  }
}

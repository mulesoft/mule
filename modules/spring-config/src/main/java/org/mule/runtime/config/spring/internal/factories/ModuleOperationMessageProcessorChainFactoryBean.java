/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.internal.factories;

import static java.lang.String.format;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.util.IdempotentExtensionWalker;
import org.mule.runtime.core.privileged.processor.objectfactory.MessageProcessorChainObjectFactory;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.processor.MessageProcessorChainBuilder;
import org.mule.runtime.core.internal.processor.chain.ModuleOperationMessageProcessorChainBuilder;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ModuleOperationMessageProcessorChainFactoryBean extends MessageProcessorChainObjectFactory {

  private Map<String, String> properties = new HashMap<>();
  private Map<String, String> parameters = new HashMap<>();
  private String moduleName;
  private String moduleOperation;
  @Inject
  private ExtensionManager extensionManager;

  @Override
  protected MessageProcessorChainBuilder getBuilderInstance() {
    final Optional<ExtensionModel> extensionModel = extensionManager.getExtensions().stream()
        .filter(em -> em.getXmlDslModel().getPrefix().equals(moduleName))
        .findFirst();
    if (!extensionModel.isPresent()) {
      throw new IllegalArgumentException(format("Could not find any extension under the name of [%s]", moduleName));
    }

    OperationSeeker operationSeeker = new OperationSeeker();
    operationSeeker.walk(extensionModel.get());
    if (!operationSeeker.operationModel.isPresent()) {
      throw new IllegalArgumentException(format("Could not find any operation under the name of [%s] for the extension [%s]",
                                                moduleOperation, moduleName));
    }
    MessageProcessorChainBuilder builder =
        new ModuleOperationMessageProcessorChainBuilder(properties, parameters, extensionModel.get(),
                                                        operationSeeker.operationModel.get(),
                                                        muleContext.getExpressionManager());
    return builder;
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

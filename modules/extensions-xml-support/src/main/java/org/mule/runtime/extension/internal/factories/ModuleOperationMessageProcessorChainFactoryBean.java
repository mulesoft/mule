/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.factories;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static org.mule.runtime.config.internal.dsl.model.extension.xml.MacroExpansionModuleModel.MODULE_OPERATION_CONFIG_REF;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.getProcessingStrategy;
import static org.mule.runtime.core.privileged.processor.chain.DefaultMessageProcessorChainBuilder.newLazyProcessorChainBuilder;

import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.processor.chain.ModuleOperationMessageProcessorChainBuilder;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChainBuilder;
import org.mule.runtime.core.privileged.processor.objectfactory.MessageProcessorChainObjectFactory;
import org.mule.runtime.extension.internal.config.dsl.XmlSdkConfigurationProvider;

import java.util.Map;

import javax.inject.Inject;

public class ModuleOperationMessageProcessorChainFactoryBean extends MessageProcessorChainObjectFactory {

  private Map<String, String> parameters = emptyMap();

  private ExtensionModel extensionModel;
  private OperationModel operationModel;

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
    return new ModuleOperationMessageProcessorChainBuilder(getProperties(), parameters, extensionModel,
                                                           operationModel,
                                                           muleContext.getExpressionManager());
  }


  public Map<String, String> getProperties() {
    // `properties` in the scope of a Xml-Sdk operation means the parameters of the config.
    if (parameters.containsKey(MODULE_OPERATION_CONFIG_REF)) {
      return extensionManager.getConfigurationProvider(parameters.get(MODULE_OPERATION_CONFIG_REF))
          .filter(cp -> cp instanceof XmlSdkConfigurationProvider)
          .map(cp -> ((XmlSdkConfigurationProvider) cp).getParameters())
          .orElse(emptyMap());
    } else {
      return emptyMap();
    }
  }

  public void setParameters(Map<String, String> parameters) {
    this.parameters = parameters;
  }

  public void setExtensionModel(ExtensionModel extensionModel) {
    this.extensionModel = extensionModel;
  }

  public void setOperationModel(OperationModel operationModel) {
    this.operationModel = operationModel;
  }
}

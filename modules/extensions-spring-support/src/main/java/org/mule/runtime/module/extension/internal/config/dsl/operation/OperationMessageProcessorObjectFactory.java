/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.operation;

import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.mule.runtime.core.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.policy.PolicyManager;
import org.mule.runtime.extension.api.model.property.PagedOperationModelProperty;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;
import org.mule.runtime.module.extension.internal.config.dsl.AbstractExtensionObjectFactory;
import org.mule.runtime.module.extension.internal.manager.ExtensionManagerAdapter;
import org.mule.runtime.module.extension.internal.model.property.InterceptingModelProperty;
import org.mule.runtime.module.extension.internal.runtime.operation.InterceptingOperationMessageProcessor;
import org.mule.runtime.module.extension.internal.runtime.operation.OperationMessageProcessor;
import org.mule.runtime.module.extension.internal.runtime.operation.PagedOperationMessageProcessor;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;

import javax.inject.Inject;

/**
 * An {@link AbstractExtensionObjectFactory} which produces {@link OperationMessageProcessor} instances
 *
 * @since 4.0
 */
public class OperationMessageProcessorObjectFactory extends AbstractExtensionObjectFactory<OperationMessageProcessor> {

  private final ExtensionModel extensionModel;
  private final OperationModel operationModel;
  private final PolicyManager policyManager;

  private ConfigurationProvider configurationProvider;
  private String target = EMPTY;

  public OperationMessageProcessorObjectFactory(ExtensionModel extensionModel, OperationModel operationModel,
                                                MuleContext muleContext, PolicyManager policyManager) {
    super(muleContext);
    this.extensionModel = extensionModel;
    this.operationModel = operationModel;
    this.policyManager = policyManager;
  }

  @Override
  public OperationMessageProcessor getObject() throws Exception {
    return withContextClassLoader(getClassLoader(extensionModel), () -> {
      try {
        ResolverSet resolverSet = getParametersAsResolverSet(operationModel);
        OperationMessageProcessor processor = createMessageProcessor(resolverSet);

        // TODO: MULE-5002 this should not be necessary but lifecycle issues when injecting message processors automatically
        muleContext.getInjector().inject(processor);
        return processor;
      } catch (Exception e) {
        throw new MuleRuntimeException(e);
      }
    });
  }

  private OperationMessageProcessor createMessageProcessor(ResolverSet resolverSet) {
    if (operationModel.getModelProperty(InterceptingModelProperty.class).isPresent()) {
      return new InterceptingOperationMessageProcessor(extensionModel, operationModel, configurationProvider, target,
                                                       resolverSet, (ExtensionManagerAdapter) muleContext.getExtensionManager(),
                                                       policyManager);
    } else if (operationModel.getModelProperty(PagedOperationModelProperty.class).isPresent()) {
      return new PagedOperationMessageProcessor(extensionModel, operationModel, configurationProvider, target, resolverSet,
                                                (ExtensionManagerAdapter) muleContext.getExtensionManager(), policyManager);
    } else {
      return new OperationMessageProcessor(extensionModel, operationModel, configurationProvider, target, resolverSet,
                                           (ExtensionManagerAdapter) muleContext.getExtensionManager(), policyManager);
    }
  }

  public void setConfigurationProvider(ConfigurationProvider configurationProvider) {
    this.configurationProvider = configurationProvider;
  }

  public void setTarget(String target) {
    this.target = target;
  }
}

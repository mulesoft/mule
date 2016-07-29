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
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.extension.api.introspection.RuntimeExtensionModel;
import org.mule.runtime.extension.api.introspection.operation.RuntimeOperationModel;
import org.mule.runtime.module.extension.internal.config.dsl.AbstractExtensionObjectFactory;
import org.mule.runtime.module.extension.internal.manager.ExtensionManagerAdapter;
import org.mule.runtime.module.extension.internal.model.property.InterceptingModelProperty;
import org.mule.runtime.module.extension.internal.runtime.operation.InterceptingOperationMessageProcessor;
import org.mule.runtime.module.extension.internal.runtime.operation.OperationMessageProcessor;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;

/**
 * An {@link AbstractExtensionObjectFactory} which produces {@link OperationMessageProcessor} instances
 *
 * @since 4.0
 */
public class OperationMessageProcessorObjectFactory extends AbstractExtensionObjectFactory<OperationMessageProcessor>
{

    private final RuntimeExtensionModel extensionModel;
    private final RuntimeOperationModel operationModel;
    private final MuleContext muleContext;

    private String configurationProviderName;
    private String target = EMPTY;

    public OperationMessageProcessorObjectFactory(RuntimeExtensionModel extensionModel, RuntimeOperationModel operationModel, MuleContext muleContext)
    {
        this.extensionModel = extensionModel;
        this.operationModel = operationModel;
        this.muleContext = muleContext;
    }

    @Override
    public OperationMessageProcessor getObject() throws Exception
    {
        return withContextClassLoader(getClassLoader(extensionModel), () ->
        {
            try
            {
                ResolverSet resolverSet = getParametersAsResolverSet(operationModel);
                OperationMessageProcessor processor = createMessageProcessor(resolverSet);

                //TODO: MULE-5002 this should not be necessary but lifecycle issues when injecting message processors automatically
                muleContext.getInjector().inject(processor);
                return processor;
            }
            catch (Exception e)
            {
                throw new MuleRuntimeException(e);
            }
        });
    }

    private OperationMessageProcessor createMessageProcessor(ResolverSet resolverSet)
    {
        if (operationModel.getModelProperty(InterceptingModelProperty.class).isPresent())
        {
            return new InterceptingOperationMessageProcessor(extensionModel,
                                                             operationModel,
                                                             configurationProviderName,
                                                             target,
                                                             resolverSet,
                                                             (ExtensionManagerAdapter) muleContext.getExtensionManager());
        }
        else
        {
            return new OperationMessageProcessor(extensionModel,
                                                 operationModel,
                                                 configurationProviderName,
                                                 target,
                                                 resolverSet,
                                                 (ExtensionManagerAdapter) muleContext.getExtensionManager());
        }
    }

    public void setConfigurationProviderName(String configurationProviderName)
    {
        this.configurationProviderName = configurationProviderName;
    }

    public void setTarget(String target)
    {
        this.target = target;
    }
}

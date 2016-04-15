/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.config;

import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.module.extension.internal.ExtensionProperties.TARGET_ATTRIBUTE;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.extension.api.introspection.RuntimeExtensionModel;
import org.mule.extension.api.introspection.operation.RuntimeOperationModel;
import org.mule.module.extension.internal.manager.ExtensionManagerAdapter;
import org.mule.module.extension.internal.runtime.processor.OperationMessageProcessor;
import org.mule.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.core.util.ObjectNameHelper;
import org.mule.runtime.core.util.StringUtils;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.FactoryBean;

/**
 * A {@link FactoryBean} which yields instances of {@link OperationMessageProcessor}
 *
 * @since 3.7.0
 */
public class OperationMessageProcessorFactoryBean extends ExtensionComponentFactoryBean<OperationMessageProcessor>
{

    private final String configurationProviderName;
    private final RuntimeOperationModel operationModel;
    private final ElementDescriptor element;
    private final ExtensionManagerAdapter extensionManager;
    private final Map<String, List<MessageProcessor>> nestedOperations;
    private final String target;
    private final MuleContext muleContext;

    public OperationMessageProcessorFactoryBean(String configurationProviderName,
                                                RuntimeExtensionModel extensionModel,
                                                RuntimeOperationModel operationModel,
                                                ElementDescriptor element,
                                                Map<String, List<MessageProcessor>> nestedOperations,
                                                MuleContext muleContext)
    {
        this.configurationProviderName = configurationProviderName;
        this.extensionModel = extensionModel;
        this.operationModel = operationModel;
        this.element = element;
        this.extensionManager = (ExtensionManagerAdapter) muleContext.getExtensionManager();
        this.nestedOperations = nestedOperations;
        this.muleContext = muleContext;

        registerNestedProcessors(nestedOperations, muleContext);
        target = getTarget(element);
    }

    @Override
    public OperationMessageProcessor getObject() throws Exception
    {
        ResolverSet resolverSet = parserDelegate.getResolverSet(element, operationModel.getParameterModels(), nestedOperations);
        OperationMessageProcessor processor = new OperationMessageProcessor(extensionModel, operationModel, configurationProviderName, target, resolverSet, extensionManager);

        //TODO: MULE-5002 this should not be necessary but lifecycle issues when injecting message processors automatically
        muleContext.getInjector().inject(processor);

        return processor;
    }

    private String getTarget(ElementDescriptor element)
    {
        String target = element.getAttribute(TARGET_ATTRIBUTE);
        return StringUtils.isBlank(target) ? EMPTY : target;
    }

    /**
     * @return {@link OperationMessageProcessor}
     */
    @Override
    public Class<?> getObjectType()
    {
        return OperationMessageProcessor.class;
    }

    /**
     * @return {@code false}
     */
    @Override
    public boolean isSingleton()
    {
        return false;
    }

    private void registerNestedProcessors(Map<String, List<MessageProcessor>> nestedOperations, MuleContext muleContext)
    {
        if (!nestedOperations.isEmpty())
        {
            ObjectNameHelper objectNameHelper = new ObjectNameHelper(muleContext);
            for (List<MessageProcessor> messageProcessors : nestedOperations.values())
            {
                try
                {
                    for (MessageProcessor messageProcessor : messageProcessors)
                    {
                        muleContext.getRegistry().registerObject(objectNameHelper.getUniqueName(""), messageProcessor);
                    }

                }
                catch (RegistrationException e)
                {
                    throw new MuleRuntimeException(createStaticMessage("Could not register nested processor"), e);
                }
            }
        }
    }
}

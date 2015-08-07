/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.config;

import static org.mule.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.module.extension.internal.config.XmlExtensionParserUtils.getResolverSet;
import org.mule.api.MuleContext;
import org.mule.api.MuleRuntimeException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.registry.RegistrationException;
import org.mule.extension.introspection.Extension;
import org.mule.extension.introspection.Operation;
import org.mule.module.extension.internal.manager.ExtensionManagerAdapter;
import org.mule.module.extension.internal.runtime.processor.OperationMessageProcessor;
import org.mule.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.util.ObjectNameHelper;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.FactoryBean;

/**
 * A {@link FactoryBean} which yields instances of {@link OperationMessageProcessor}
 *
 * @since 3.7.0
 */
public class OperationMessageProcessorFactoryBean implements FactoryBean<OperationMessageProcessor>
{

    private final String configurationInstanceProviderName;
    private final Extension extension;
    private final Operation operation;
    private final ElementDescriptor element;
    private final ExtensionManagerAdapter extensionManager;
    private final Map<String, List<MessageProcessor>> nestedOperations;

    public OperationMessageProcessorFactoryBean(String configurationInstanceProviderName,
                                                Extension extension,
                                                Operation operation,
                                                ElementDescriptor element,
                                                Map<String, List<MessageProcessor>> nestedOperations,
                                                MuleContext muleContext)
    {
        this.configurationInstanceProviderName = configurationInstanceProviderName;
        this.extension = extension;
        this.operation = operation;
        this.element = element;
        this.extensionManager = (ExtensionManagerAdapter) muleContext.getExtensionManager();
        this.nestedOperations = nestedOperations;

        registerNestedProcessors(nestedOperations, muleContext);

    }

    @Override
    public OperationMessageProcessor getObject() throws Exception
    {
        ResolverSet resolverSet = getResolverSet(element, operation.getParameters(), nestedOperations);
        return new OperationMessageProcessor(extension, operation, configurationInstanceProviderName, resolverSet, extensionManager);
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
     * @return {@value false}
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

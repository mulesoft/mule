/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import static org.mule.runtime.core.api.config.MuleProperties.DEFAULT_LOCAL_TRANSIENT_USER_OBJECT_STORE_NAME;
import static org.mule.runtime.core.api.config.MuleProperties.DEFAULT_LOCAL_USER_OBJECT_STORE_NAME;
import static org.mule.runtime.core.api.config.MuleProperties.DEFAULT_USER_OBJECT_STORE_NAME;
import static org.mule.runtime.core.api.config.MuleProperties.DEFAULT_USER_TRANSIENT_OBJECT_STORE_NAME;
import static org.mule.runtime.core.api.config.MuleProperties.LOCAL_OBJECT_STORE_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONNECTION_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONNECTOR_MESSAGE_PROCESSOR_LOCATOR;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONVERTER_RESOLVER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_DEFAULT_MESSAGE_DISPATCHER_THREADING_PROFILE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_DEFAULT_MESSAGE_PROCESSING_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_DEFAULT_MESSAGE_RECEIVER_THREADING_PROFILE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_DEFAULT_MESSAGE_REQUESTER_THREADING_PROFILE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_DEFAULT_RETRY_POLICY_TEMPLATE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_DEFAULT_SERVICE_THREADING_PROFILE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_DEFAULT_THREADING_PROFILE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXCEPTION_LOCATION_PROVIDER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXPRESSION_LANGUAGE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXTENSION_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_LOCAL_QUEUE_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_LOCAL_STORE_IN_MEMORY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_LOCAL_STORE_PERSISTENT;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_LOCK_FACTORY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_LOCK_PROVIDER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MESSAGE_PROCESSING_FLOW_TRACE_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_METADATA_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONFIGURATION;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_STREAM_CLOSER_SERVICE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_NOTIFICATION_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_OBJECT_NAME_PROCESSOR;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_PROCESSING_TIME_WATCHER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_QUEUE_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_SECURITY_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_SERIALIZER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_DEFAULT_IN_MEMORY_NAME;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_DEFAULT_PERSISTENT_NAME;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_TIME_SUPPLIER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_TRANSACTION_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.QUEUE_STORE_DEFAULT_IN_MEMORY_NAME;
import static org.mule.runtime.core.api.config.MuleProperties.QUEUE_STORE_DEFAULT_PERSISTENT_NAME;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;
import org.mule.runtime.config.spring.factories.ConstantFactoryBean;
import org.mule.runtime.config.spring.factories.ExtensionManagerFactoryBean;
import org.mule.runtime.config.spring.factories.TransactionManagerFactoryBean;
import org.mule.runtime.config.spring.processors.MuleObjectNameProcessor;
import org.mule.runtime.config.spring.processors.ParentContextPropertyPlaceholderProcessor;
import org.mule.runtime.config.spring.processors.PropertyPlaceholderProcessor;
import org.mule.runtime.core.DynamicDataTypeConversionResolver;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.notification.ConnectionNotificationListener;
import org.mule.runtime.core.api.context.notification.CustomNotificationListener;
import org.mule.runtime.core.api.context.notification.ExceptionNotificationListener;
import org.mule.runtime.core.api.context.notification.ManagementNotificationListener;
import org.mule.runtime.core.api.context.notification.MuleContextNotificationListener;
import org.mule.runtime.core.api.context.notification.RegistryNotificationListener;
import org.mule.runtime.core.api.context.notification.SecurityNotificationListener;
import org.mule.runtime.core.api.context.notification.TransactionNotificationListener;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.config.ChainedThreadingProfile;
import org.mule.runtime.core.config.bootstrap.ArtifactType;
import org.mule.runtime.core.config.factories.HostNameFactory;
import org.mule.runtime.core.connector.MuleConnectorOperationLocator;
import org.mule.runtime.core.context.notification.ConnectionNotification;
import org.mule.runtime.core.context.notification.CustomNotification;
import org.mule.runtime.core.context.notification.ExceptionNotification;
import org.mule.runtime.core.context.notification.ManagementNotification;
import org.mule.runtime.core.context.notification.MessageProcessingFlowTraceManager;
import org.mule.runtime.core.context.notification.MuleContextNotification;
import org.mule.runtime.core.context.notification.RegistryNotification;
import org.mule.runtime.core.context.notification.SecurityNotification;
import org.mule.runtime.core.context.notification.TransactionNotification;
import org.mule.runtime.core.el.mvel.MVELExpressionLanguageWrapper;
import org.mule.runtime.core.exception.MessagingExceptionLocationProvider;
import org.mule.runtime.core.execution.MuleMessageProcessingManager;
import org.mule.runtime.core.internal.connection.DefaultConnectionManager;
import org.mule.runtime.core.internal.metadata.MuleMetadataManager;
import org.mule.runtime.core.management.stats.DefaultProcessingTimeWatcher;
import org.mule.runtime.core.retry.policies.NoRetryPolicyTemplate;
import org.mule.runtime.core.security.MuleSecurityManager;
import org.mule.runtime.core.time.TimeSupplier;
import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.core.util.DefaultStreamCloserService;
import org.mule.runtime.core.util.lock.MuleLockFactory;
import org.mule.runtime.core.util.lock.SingleServerLockProvider;
import org.mule.runtime.core.util.queue.DelegateQueueManager;
import org.mule.runtime.core.util.store.DefaultObjectStoreFactoryBean;
import org.mule.runtime.core.util.store.MuleObjectStoreManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

/**
 * @since 4.0
 */
class SpringMuleContextServiceConfigurator
{

    void createArtifactServices(BeanDefinitionRegistry beanDefinitionRegistry, MuleContext muleContext, ArtifactType artifactType, OptionalObjectsController optionalObjectsController)
    {
        createBootstrapBeanDefinitions(beanDefinitionRegistry, muleContext, artifactType, optionalObjectsController);
        createNotificationManager(beanDefinitionRegistry);
        createObjectStoreBeanDefinitions(beanDefinitionRegistry);
        createQueueStoreBeanDefinitions(beanDefinitionRegistry);
        createQueueManagerBeanDefinitions(beanDefinitionRegistry);
        createThreadingProfileBeanDefinitions(beanDefinitionRegistry);
        createSpringSpecificBeanDefinitions(beanDefinitionRegistry);

        beanDefinitionRegistry.registerBeanDefinition(OBJECT_TRANSACTION_MANAGER, getBeanDefinition(TransactionManagerFactoryBean.class));
        beanDefinitionRegistry.registerBeanDefinition(OBJECT_DEFAULT_RETRY_POLICY_TEMPLATE, getBeanDefinition(NoRetryPolicyTemplate.class));
        beanDefinitionRegistry.registerBeanDefinition(OBJECT_EXPRESSION_LANGUAGE, getBeanDefinition(MVELExpressionLanguageWrapper.class));
        beanDefinitionRegistry.registerBeanDefinition(OBJECT_EXTENSION_MANAGER, getBeanDefinition(ExtensionManagerFactoryBean.class));
        beanDefinitionRegistry.registerBeanDefinition(OBJECT_TIME_SUPPLIER, getBeanDefinition(TimeSupplier.class));
        beanDefinitionRegistry.registerBeanDefinition(OBJECT_CONNECTION_MANAGER, getBeanDefinition(DefaultConnectionManager.class));
        beanDefinitionRegistry.registerBeanDefinition(OBJECT_METADATA_MANAGER, getBeanDefinition(MuleMetadataManager.class));
        beanDefinitionRegistry.registerBeanDefinition(OBJECT_OBJECT_NAME_PROCESSOR, getBeanDefinition(MuleObjectNameProcessor.class));
        beanDefinitionRegistry.registerBeanDefinition(OBJECT_SERIALIZER, getBeanDefinitionBuilder(DefaultObjectSerializerFactoryBean.class)
                .addDependsOn(OBJECT_MULE_CONFIGURATION)
                .getBeanDefinition());

        if (artifactType.equals(ArtifactType.APP))
        {
            createApplicationServicesBeanDefinitions(beanDefinitionRegistry);
        }

        createEndpointFactory(beanDefinitionRegistry);
    }

    private void createQueueStoreBeanDefinitions(BeanDefinitionRegistry beanDefinitionRegistry)
    {
        beanDefinitionRegistry.registerBeanDefinition(QUEUE_STORE_DEFAULT_PERSISTENT_NAME, getBeanDefinition(DefaultObjectStoreFactoryBean.class, "createDefaultPersistentQueueStore"));
        beanDefinitionRegistry.registerAlias(QUEUE_STORE_DEFAULT_PERSISTENT_NAME, "_fileQueueStore");
        beanDefinitionRegistry.registerBeanDefinition(QUEUE_STORE_DEFAULT_IN_MEMORY_NAME, getBeanDefinition(DefaultObjectStoreFactoryBean.class, "createDefaultInMemoryQueueStore"));
        beanDefinitionRegistry.registerAlias(QUEUE_STORE_DEFAULT_IN_MEMORY_NAME, "_simpleMemoryQueueStore");
    }

    private void createApplicationServicesBeanDefinitions(BeanDefinitionRegistry beanDefinitionRegistry)
    {
        beanDefinitionRegistry.registerBeanDefinition(OBJECT_SECURITY_MANAGER, getBeanDefinition(MuleSecurityManager.class));
        beanDefinitionRegistry.registerBeanDefinition(OBJECT_DEFAULT_MESSAGE_PROCESSING_MANAGER, getBeanDefinition(MuleMessageProcessingManager.class));
        beanDefinitionRegistry.registerBeanDefinition(OBJECT_MULE_STREAM_CLOSER_SERVICE, getBeanDefinition(DefaultStreamCloserService.class));
        beanDefinitionRegistry.registerBeanDefinition(OBJECT_MULE_STREAM_CLOSER_SERVICE, getBeanDefinition(DefaultStreamCloserService.class));
        beanDefinitionRegistry.registerBeanDefinition(OBJECT_CONVERTER_RESOLVER, getBeanDefinition(DynamicDataTypeConversionResolver.class)); //missing muleContext in contrcutor
        beanDefinitionRegistry.registerBeanDefinition(OBJECT_LOCK_FACTORY, getBeanDefinition(MuleLockFactory.class));
        beanDefinitionRegistry.registerBeanDefinition(OBJECT_LOCK_PROVIDER, getBeanDefinition(SingleServerLockProvider.class));
        beanDefinitionRegistry.registerBeanDefinition(OBJECT_PROCESSING_TIME_WATCHER, getBeanDefinition(DefaultProcessingTimeWatcher.class));
        beanDefinitionRegistry.registerBeanDefinition(OBJECT_CONNECTOR_MESSAGE_PROCESSOR_LOCATOR, getBeanDefinition(MuleConnectorOperationLocator.class));
        beanDefinitionRegistry.registerBeanDefinition(OBJECT_EXCEPTION_LOCATION_PROVIDER, getBeanDefinition(MessagingExceptionLocationProvider.class));
        beanDefinitionRegistry.registerBeanDefinition(OBJECT_MESSAGE_PROCESSING_FLOW_TRACE_MANAGER, getBeanDefinition(MessageProcessingFlowTraceManager.class));
    }

    private void createSpringSpecificBeanDefinitions(BeanDefinitionRegistry beanDefinitionRegistry)
    {
        beanDefinitionRegistry.registerBeanDefinition("_muleParentContextPropertyPlaceholderProcessor", getBeanDefinition(ParentContextPropertyPlaceholderProcessor.class));
        HashMap<Object, Object> factories = new HashMap<>();
        factories.put("hostname", new HostNameFactory());
        beanDefinitionRegistry.registerBeanDefinition("_mulePropertyPlaceholderProcessor", getBeanDefinitionBuilder(PropertyPlaceholderProcessor.class)
                .addPropertyValue("factories", factories)
                .addPropertyValue("ignoreUnresolvablePlaceholders", true)
                .getBeanDefinition());
    }

    private void createThreadingProfileBeanDefinitions(BeanDefinitionRegistry beanDefinitionRegistry)
    {
        beanDefinitionRegistry.registerBeanDefinition(OBJECT_DEFAULT_THREADING_PROFILE, getBeanDefinition(ChainedThreadingProfile.class));
        beanDefinitionRegistry.registerBeanDefinition(OBJECT_DEFAULT_SERVICE_THREADING_PROFILE, getBeanDefinition(ChainedThreadingProfile.class));
        beanDefinitionRegistry.registerBeanDefinition(OBJECT_DEFAULT_MESSAGE_DISPATCHER_THREADING_PROFILE, getBeanDefinitionBuilder(ChainedThreadingProfile.class).addConstructorArgReference(OBJECT_DEFAULT_THREADING_PROFILE).getBeanDefinition());
        beanDefinitionRegistry.registerBeanDefinition(OBJECT_DEFAULT_MESSAGE_REQUESTER_THREADING_PROFILE, getBeanDefinitionBuilder(ChainedThreadingProfile.class).addConstructorArgReference(OBJECT_DEFAULT_THREADING_PROFILE).getBeanDefinition());
        beanDefinitionRegistry.registerBeanDefinition(OBJECT_DEFAULT_MESSAGE_RECEIVER_THREADING_PROFILE, getBeanDefinitionBuilder(ChainedThreadingProfile.class).addConstructorArgReference(OBJECT_DEFAULT_THREADING_PROFILE).getBeanDefinition());
    }

    private void createQueueManagerBeanDefinitions(BeanDefinitionRegistry beanDefinitionRegistry)
    {
        beanDefinitionRegistry.registerBeanDefinition(OBJECT_QUEUE_MANAGER, getBeanDefinitionBuilder(ConstantFactoryBean.class).addConstructorArgReference(OBJECT_LOCAL_QUEUE_MANAGER).getBeanDefinition());
        beanDefinitionRegistry.registerBeanDefinition(OBJECT_LOCAL_QUEUE_MANAGER, getBeanDefinition(DelegateQueueManager.class));
    }

    private void createObjectStoreBeanDefinitions(BeanDefinitionRegistry beanDefinitionRegistry)
    {
        beanDefinitionRegistry.registerBeanDefinition(OBJECT_STORE_DEFAULT_IN_MEMORY_NAME, getBeanDefinitionBuilder(ConstantFactoryBean.class).addConstructorArgReference(OBJECT_LOCAL_STORE_IN_MEMORY).getBeanDefinition());
        beanDefinitionRegistry.registerBeanDefinition(OBJECT_LOCAL_STORE_IN_MEMORY, getBeanDefinition(DefaultObjectStoreFactoryBean.class, "createDefaultInMemoryObjectStore"));
        beanDefinitionRegistry.registerBeanDefinition(OBJECT_STORE_DEFAULT_PERSISTENT_NAME, getBeanDefinitionBuilder(ConstantFactoryBean.class).addConstructorArgReference(OBJECT_LOCAL_STORE_PERSISTENT).getBeanDefinition());
        beanDefinitionRegistry.registerBeanDefinition(OBJECT_LOCAL_STORE_PERSISTENT, getBeanDefinition(DefaultObjectStoreFactoryBean.class, "createDefaultPersistentObjectStore"));
        beanDefinitionRegistry.registerBeanDefinition(DEFAULT_USER_OBJECT_STORE_NAME, getBeanDefinitionBuilder(ConstantFactoryBean.class).addConstructorArgReference(DEFAULT_LOCAL_USER_OBJECT_STORE_NAME).getBeanDefinition());
        beanDefinitionRegistry.registerBeanDefinition(DEFAULT_LOCAL_USER_OBJECT_STORE_NAME, getBeanDefinition(DefaultObjectStoreFactoryBean.class, "createDefaultUserObjectStore"));
        beanDefinitionRegistry.registerBeanDefinition(DEFAULT_USER_TRANSIENT_OBJECT_STORE_NAME, getBeanDefinitionBuilder(ConstantFactoryBean.class).addConstructorArgReference(DEFAULT_LOCAL_TRANSIENT_USER_OBJECT_STORE_NAME).getBeanDefinition());
        beanDefinitionRegistry.registerBeanDefinition(DEFAULT_LOCAL_TRANSIENT_USER_OBJECT_STORE_NAME, getBeanDefinition(DefaultObjectStoreFactoryBean.class, "createDefaultUserTransientObjectStore"));
        beanDefinitionRegistry.registerBeanDefinition(OBJECT_STORE_MANAGER, getBeanDefinition(MuleObjectStoreManager.class));
        beanDefinitionRegistry.registerAlias(OBJECT_STORE_MANAGER, LOCAL_OBJECT_STORE_MANAGER);
    }

    private void createEndpointFactory(BeanDefinitionRegistry beanDefinitionRegistry)
    {
        try
        {
            Class endpointFactoryClass = ClassUtils.loadClass("org.mule.compatibility.core.endpoint.DefaultEndpointFactory", Thread.currentThread().getContextClassLoader());
            beanDefinitionRegistry.registerBeanDefinition("_muleEndpointFactory", getBeanDefinition(endpointFactoryClass));
        }
        catch (ClassNotFoundException e)
        {
            //Nothing to do.
        }
    }

    private void createNotificationManager(BeanDefinitionRegistry beanDefinitionRegistry)
    {
        List<Notification> defaultNotifications = new ArrayList<>();
        defaultNotifications.add(new Notification(MuleContextNotificationListener.class, MuleContextNotification.class));
        defaultNotifications.add(new Notification(SecurityNotificationListener.class, SecurityNotification.class));
        defaultNotifications.add(new Notification(ManagementNotificationListener.class, ManagementNotification.class));
        defaultNotifications.add(new Notification(ConnectionNotificationListener.class, ConnectionNotification.class));
        defaultNotifications.add(new Notification(RegistryNotificationListener.class, RegistryNotification.class));
        defaultNotifications.add(new Notification(CustomNotificationListener.class, CustomNotification.class));
        defaultNotifications.add(new Notification(ExceptionNotificationListener.class, ExceptionNotification.class));
        defaultNotifications.add(new Notification(TransactionNotificationListener.class, TransactionNotification.class));
        beanDefinitionRegistry.registerBeanDefinition(OBJECT_NOTIFICATION_MANAGER, getBeanDefinitionBuilder(ServerNotificationManagerConfigurator.class)
                .addPropertyValue("enabledNotifications", defaultNotifications)
                .getBeanDefinition());
    }

    private void createBootstrapBeanDefinitions(BeanDefinitionRegistry beanDefinitionRegistry, MuleContext muleContext, ArtifactType artifactType, OptionalObjectsController optionalObjectsController)
    {
        try
        {
            SpringRegistryBootstrap springRegistryBootstrap = new SpringRegistryBootstrap(artifactType, muleContext, optionalObjectsController, beanDefinitionRegistry);
            springRegistryBootstrap.initialise();
        }
        catch (InitialisationException e)
        {
            throw new RuntimeException(e);
        }
    }

    private AbstractBeanDefinition getBeanDefinition(Class<?> beanType)
    {
        return getBeanDefinitionBuilder(beanType)
                .getBeanDefinition();
    }

    private BeanDefinitionBuilder getBeanDefinitionBuilder(Class<?> beanType)
    {
        return genericBeanDefinition(beanType);
    }

    private AbstractBeanDefinition getBeanDefinition(Class<?> beanType, String factoryMethodName)
    {
        return getBeanDefinitionBuilder(beanType)
                .setFactoryMethod(factoryMethodName)
                .getBeanDefinition();
    }

}

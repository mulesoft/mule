/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.application;

import org.mule.DataTypeConversionResolver;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.SingleResourceTransactionFactoryManager;
import org.mule.api.client.LocalMuleClient;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.context.WorkManager;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.context.notification.ServerNotificationListener;
import org.mule.api.el.ExpressionLanguage;
import org.mule.api.endpoint.EndpointFactory;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.exception.RollbackSourceCallback;
import org.mule.api.exception.SystemExceptionHandler;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleManager;
import org.mule.api.registry.MuleRegistry;
import org.mule.api.registry.RegistrationException;
import org.mule.api.registry.Registry;
import org.mule.api.security.SecurityManager;
import org.mule.api.store.ListableObjectStore;
import org.mule.api.store.ObjectStoreManager;
import org.mule.context.notification.NotificationException;
import org.mule.context.notification.ServerNotificationManager;
import org.mule.management.stats.AllStatistics;
import org.mule.management.stats.ProcessingTimeWatcher;
import org.mule.module.launcher.artifact.ArtifactClassLoader;
import org.mule.util.lock.LockFactory;
import org.mule.util.queue.QueueManager;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import javax.resource.spi.work.WorkListener;
import javax.transaction.TransactionManager;
import javax.xml.namespace.QName;

/**
 * Delegates the invocation to the correct MuleContext
 * based on the current class loader
 */
public class ArtifactMuleContextDelegate implements MuleContext
{

    @Override
    public void setTransactionManager(TransactionManager manager) throws Exception
    {
        getDelegate().setTransactionManager(manager);
    }

    @Override
    public TransactionManager getTransactionManager()
    {
        return getDelegate().getTransactionManager();
    }

    @Override
    public ServerNotificationManager getNotificationManager()
    {
        return getDelegate().getNotificationManager();
    }

    @Override
    public boolean isStarted()
    {
        return getDelegate().isStarted();
    }

    @Override
    public boolean isInitialised()
    {
        return getDelegate().isInitialised();
    }

    @Override
    public boolean isInitialising()
    {
        return getDelegate().isInitialising();
    }

    @Override
    public boolean isDisposed()
    {
        return getDelegate().isDisposed();
    }

    @Override
    public boolean isDisposing()
    {
        return getDelegate().isDisposing();
    }

    @Override
    public void registerListener(ServerNotificationListener l) throws NotificationException
    {
        getDelegate().registerListener(l);
    }

    @Override
    public void registerListener(ServerNotificationListener l, String resourceIdentifier) throws NotificationException
    {
        getDelegate().registerListener(l, resourceIdentifier);
    }

    @Override
    public void unregisterListener(ServerNotificationListener l)
    {
        getDelegate().unregisterListener(l);
    }

    @Override
    public void fireNotification(ServerNotification notification)
    {
        getDelegate().fireNotification(notification);
    }

    @Override
    public void setSecurityManager(org.mule.api.security.SecurityManager securityManager) throws InitialisationException, RegistrationException
    {
        getDelegate().setSecurityManager(securityManager);
    }

    @Override
    public SecurityManager getSecurityManager()
    {
        return getDelegate().getSecurityManager();
    }

    @Override
    public WorkManager getWorkManager()
    {
        return getDelegate().getWorkManager();
    }

    @Override
    public WorkListener getWorkListener()
    {
        return getDelegate().getWorkListener();
    }

    @Override
    public void setQueueManager(QueueManager queueManager) throws RegistrationException
    {
        getDelegate().setQueueManager(queueManager);
    }

    @Override
    public QueueManager getQueueManager()
    {
        return getDelegate().getQueueManager();
    }

    @Override
    public ObjectStoreManager getObjectStoreManager()
    {
        return getDelegate().getObjectStoreManager();
    }

    @Override
    public AllStatistics getStatistics()
    {
        return getDelegate().getStatistics();
    }

    @Override
    public LifecycleManager getLifecycleManager()
    {
        return getDelegate().getLifecycleManager();
    }

    @Override
    public MuleRegistry getRegistry()
    {
        return getDelegate().getRegistry();
    }

    @Override
    public MuleConfiguration getConfiguration()
    {
        return getDelegate().getConfiguration();
    }

    @Override
    public ThreadingProfile getDefaultMessageDispatcherThreadingProfile()
    {
        return getDelegate().getDefaultMessageDispatcherThreadingProfile();
    }

    @Override
    public ThreadingProfile getDefaultMessageRequesterThreadingProfile()
    {
        return getDelegate().getDefaultMessageRequesterThreadingProfile();
    }

    @Override
    public ThreadingProfile getDefaultMessageReceiverThreadingProfile()
    {
        return getDelegate().getDefaultMessageReceiverThreadingProfile();
    }

    @Override
    public ThreadingProfile getDefaultServiceThreadingProfile()
    {
        return getDelegate().getDefaultServiceThreadingProfile();
    }

    @Override
    public ThreadingProfile getDefaultThreadingProfile()
    {
        return getDelegate().getDefaultThreadingProfile();
    }

    @Override
    public void addRegistry(Registry registry)
    {
        getDelegate().addRegistry(registry);
    }

    @Override
    public void removeRegistry(Registry registry)
    {
        getDelegate().removeRegistry(registry);
    }

    @Override
    public long getStartDate()
    {
        return getDelegate().getStartDate();
    }

    @Override
    public ExpressionManager getExpressionManager()
    {
        return getDelegate().getExpressionManager();
    }

    @Override
    public EndpointFactory getEndpointFactory()
    {
        return getDelegate().getEndpointFactory();
    }

    @Override
    public void setExecutionClassLoader(ClassLoader cl)
    {
        getDelegate().setExecutionClassLoader(cl);
    }

    @Override
    public ClassLoader getExecutionClassLoader()
    {
        return getDelegate().getExecutionClassLoader();
    }

    @Override
    public boolean isStopped()
    {
        return getDelegate().isStopped();
    }

    @Override
    public boolean isStopping()
    {
        return getDelegate().isStopping();
    }

    @Override
    public boolean isStarting()
    {
        return getDelegate().isStarting();
    }

    @Override
    public LocalMuleClient getClient()
    {
        return getDelegate().getClient();
    }

    @Override
    public SystemExceptionHandler getExceptionListener()
    {
        return getDelegate().getExceptionListener();
    }

    @Override
    public void setExceptionListener(SystemExceptionHandler exceptionListener)
    {
        getDelegate().setExceptionListener(exceptionListener);
    }

    @Override
    public void setObjectStore(String name, ListableObjectStore<Serializable> store) throws RegistrationException
    {
        getDelegate().setObjectStore(name, store);
    }

    @Override
    public void handleException(Exception e, RollbackSourceCallback rollbackMethod)
    {
        getDelegate().handleException(e, rollbackMethod);
    }

    @Override
    public void handleException(Exception e)
    {
        getDelegate().handleException(e);
    }

    @Override
    public String getClusterId()
    {
        return getDelegate().getClusterId();
    }

    @Override
    public int getClusterNodeId()
    {
        return getDelegate().getClusterNodeId();
    }

    @Override
    public boolean isPrimaryPollingInstance()
    {
        return getDelegate().isPrimaryPollingInstance();
    }

    @Override
    public String getUniqueIdString()
    {
        return getDelegate().getUniqueIdString();
    }

    @Override
    public Map<QName, Set<Object>> getConfigurationAnnotations()
    {
        return getDelegate().getConfigurationAnnotations();
    }

    @Override
    public MessagingExceptionHandler getDefaultExceptionStrategy()
    {
        return getDelegate().getDefaultExceptionStrategy();
    }

    @Override
    public SingleResourceTransactionFactoryManager getTransactionFactoryManager()
    {
        return getDelegate().getTransactionFactoryManager();
    }

    @Override
    public DataTypeConversionResolver getDataTypeConverterResolver()
    {
        return getDelegate().getDataTypeConverterResolver();
    }

    @Override
    public ExpressionLanguage getExpressionLanguage()
    {
        return getDelegate().getExpressionLanguage();
    }

    @Override
    public LockFactory getLockFactory()
    {
        return getDelegate().getLockFactory();
    }

    @Override
    public ProcessingTimeWatcher getProcessorTimeWatcher()
    {
        return getDelegate().getProcessorTimeWatcher();
    }

    @Override
    public void initialise() throws InitialisationException
    {
        getDelegate().initialise();
    }

    @Override
    public void start() throws MuleException
    {
        getDelegate().start();
    }

    @Override
    public void stop() throws MuleException
    {
        getDelegate().stop();
    }

    @Override
    public void dispose()
    {
        getDelegate().dispose();
    }

    public MuleContext getDelegate()
    {
        ArtifactClassLoader artifactClassLoader = (ArtifactClassLoader) Thread.currentThread().getContextClassLoader();
        return artifactClassLoader.getMuleContext();
    }

}

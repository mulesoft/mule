/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.btm.transaction;

import org.mule.api.MuleContext;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.transaction.TransactionManagerFactory;
import org.mule.module.btm.xa.DefaultXaSessionResourceProducer;
import org.mule.util.xa.AbstractXAResourceManager;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.MessageFormat;

import javax.transaction.TransactionManager;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.recovery.RecoveryException;
import bitronix.tm.resource.ResourceRegistrar;

public class BitronixTransactionManagerFactory implements TransactionManagerFactory, Disposable, MuleContextAware
{

    private static int numberOfAppsUsingTm;
    private static TransactionManager transactionManager;

    private MuleContext muleContext;
    private DefaultXaSessionResourceProducer defaultXaSessionResourceProducer;
    private boolean transactionManagerUsedByThisApp;

    public BitronixTransactionManagerFactory()
    {
    }

    public TransactionManager create(MuleConfiguration config) throws Exception
    {
        synchronized (BitronixTransactionManagerFactory.class)
        {
            if (!transactionManagerUsedByThisApp)
            {
                transactionManagerUsedByThisApp = true;
                numberOfAppsUsingTm++;
            }
            if (transactionManager != null)
            {
                return transactionManager;
            }
            configureTransactionLogsDirectory();
            configureUniqueServerId();
            transactionManager = TransactionManagerServices.getTransactionManager();
            registerMuleQueuesXaResource();
            transactionManager = new TransactionManagerWrapper(transactionManager, defaultXaSessionResourceProducer);
            return transactionManager;
        }
    }

    private void registerMuleQueuesXaResource() throws RecoveryException
    {
        String defaultXaSessionUniqueName = muleContext.getConfiguration().getId() + "-default-xa-session";
        defaultXaSessionResourceProducer = new DefaultXaSessionResourceProducer(defaultXaSessionUniqueName, (AbstractXAResourceManager) muleContext.getQueueManager());
        ResourceRegistrar.register(defaultXaSessionResourceProducer);
    }

    private void configureUniqueServerId() throws UnknownHostException
    {
        InetAddress address = InetAddress.getLocalHost();
        final String xaNodeId = MessageFormat.format("Mule[{0}/{1}]",
                                                     address.getHostName(), address.getHostAddress());
        TransactionManagerServices.getConfiguration().setServerId(xaNodeId);
    }

    private void configureTransactionLogsDirectory()
    {
        String workingDirectory = muleContext.getConfiguration().getWorkingDirectory();
        String part1Filename = workingDirectory + File.separator + "tx-log" + File.separator + TransactionManagerServices.getConfiguration().getLogPart1Filename();
        String part2Filename = workingDirectory + File.separator + "tx-log" + File.separator + TransactionManagerServices.getConfiguration().getLogPart2Filename();
        TransactionManagerServices.getConfiguration().setLogPart1Filename(part1Filename);
        TransactionManagerServices.getConfiguration().setLogPart2Filename(part2Filename);
    }

    @Override
    public void dispose()
    {
        if (transactionManagerUsedByThisApp)
        {
            synchronized (BitronixTransactionManagerFactory.class)
            {
                numberOfAppsUsingTm--;
                if (numberOfAppsUsingTm == 0)
                {
                    TransactionManagerServices.getTransactionManager().shutdown();
                }
            }
            transactionManager = null;
        }
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }
}

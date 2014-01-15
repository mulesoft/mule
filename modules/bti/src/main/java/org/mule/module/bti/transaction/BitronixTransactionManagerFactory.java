/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.bti.transaction;

import static org.mule.module.bti.BitronixConfigurationUtil.createUniqueIdForServer;
import static org.mule.module.bti.BitronixConfigurationUtil.getLogPart1Filename;
import static org.mule.module.bti.BitronixConfigurationUtil.getLogPart2Filename;

import org.mule.api.MuleContext;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.transaction.TransactionManagerFactory;
import org.mule.module.bti.BitronixConfigurationUtil;
import org.mule.module.bti.xa.DefaultXaSessionResourceProducer;
import org.mule.util.xa.AbstractXAResourceManager;

import javax.transaction.TransactionManager;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.resource.ResourceRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BitronixTransactionManagerFactory implements TransactionManagerFactory, Disposable, MuleContextAware
{

    private static final Logger logger = LoggerFactory.getLogger(BitronixTransactionManagerFactory.class);

    private static int numberOfAppsUsingTm;
    private static TransactionManager transactionManager;
    private static DefaultXaSessionResourceProducer defaultXaSessionResourceProducer;

    private MuleContext muleContext;
    private boolean transactionManagerUsedByThisApp;

    public TransactionManager create(MuleConfiguration config) throws Exception
    {
        synchronized (BitronixTransactionManagerFactory.class)
        {
            if (transactionManager == null)
            {
                configureTransactionLogsDirectory();
                configureUniqueServerId();
                configureTransactionRecoveryExecutionInterval();
                configureTransactionTimeout();
                registerMuleQueuesXaResource();
                transactionManager = TransactionManagerServices.getTransactionManager();
                transactionManager = new TransactionManagerWrapper(transactionManager, defaultXaSessionResourceProducer);
            }
            if (!transactionManagerUsedByThisApp)
            {
                transactionManagerUsedByThisApp = true;
                numberOfAppsUsingTm++;
            }
            return transactionManager;
        }
    }

    private void configureTransactionTimeout()
    {
        TransactionManagerServices.getConfiguration().setDefaultTransactionTimeout(BitronixConfigurationUtil.getTransactionTimeout());
    }

    private void configureTransactionRecoveryExecutionInterval()
    {
        int transactionRecoveryIntervalInSeconds = BitronixConfigurationUtil.getTransactionRecoveryIntervalInSeconds();
        logger.info("Using " + transactionRecoveryIntervalInSeconds + " seconds for recovery interval");
        TransactionManagerServices.getConfiguration().setBackgroundRecoveryIntervalSeconds(transactionRecoveryIntervalInSeconds);
    }

    private void registerMuleQueuesXaResource() throws Exception
    {
        String defaultXaSessionUniqueName = createUniqueIdForServer() + "-default-xa-session";
        defaultXaSessionResourceProducer = new DefaultXaSessionResourceProducer(defaultXaSessionUniqueName, (AbstractXAResourceManager) muleContext.getQueueManager());
        ResourceRegistrar.register(defaultXaSessionResourceProducer);
    }

    private void configureUniqueServerId() throws Exception
    {
        String uniqueServerId = createUniqueIdForServer();
        logger.info("Bitronix server id: " + uniqueServerId);
        TransactionManagerServices.getConfiguration().setServerId(uniqueServerId);
    }

    private void configureTransactionLogsDirectory()
    {
        String logPart1Filename = getLogPart1Filename();
        String logPart2Filename = getLogPart2Filename();
        logger.info("Using log file " + logPart1Filename + " for tx log part 1");
        logger.info("Using log file " + logPart2Filename + " for tx log part 2");
        TransactionManagerServices.getConfiguration().setLogPart1Filename(logPart1Filename);
        TransactionManagerServices.getConfiguration().setLogPart2Filename(logPart2Filename);
    }

    @Override
    public void dispose()
    {
        try
        {
            if (transactionManagerUsedByThisApp)
            {
                synchronized (BitronixTransactionManagerFactory.class)
                {
                    numberOfAppsUsingTm--;
                    transactionManagerUsedByThisApp = false;
                    if (numberOfAppsUsingTm == 0)
                    {
                        transactionManager = null;
                        defaultXaSessionResourceProducer.close();
                        TransactionManagerServices.getTransactionManager().shutdown();
                    }
                }
            }
        }
        catch (Exception e)
        {
            logger.error("Failure shutting down transaction manager" + e.getMessage());
            if (logger.isDebugEnabled())
            {
                logger.debug(e.getMessage(), e);
            }
        }
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }
}

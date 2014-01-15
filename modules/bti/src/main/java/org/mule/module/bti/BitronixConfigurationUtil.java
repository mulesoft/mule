/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.bti;

import org.mule.api.MuleContext;
import org.mule.api.config.MuleProperties;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;

import bitronix.tm.TransactionManagerServices;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BitronixConfigurationUtil
{

    private static final Logger logger = LoggerFactory.getLogger(BitronixConfigurationUtil.class);

    public static final String BITRONIX_SERVER_ID = "mule.bitronix.serverId";
    public static final String BITRONIX_RECOVERY_INTERVAL = "mule.bitronix.recoveryinterval";
    public static final String BITRONIX_TRANSACTION_TIMEOUT = "mule.bitronix.transactiontimeout";
    //We copy the cluster node id property key here to not depend on cluster module only for this constant
    public static final String CLUSTER_NODE_ID_PROPERTY = MuleProperties.SYSTEM_PROPERTY_PREFIX + "clusterNodeId";

    public static String createUniqueIdForServer()
    {
        String configuredBitronixServerId = System.getProperty(BITRONIX_SERVER_ID);
        if (configuredBitronixServerId != null)
        {
            return configuredBitronixServerId;
        }
        else
        {
            try
            {
                InetAddress address = InetAddress.getLocalHost();
                return String.valueOf(Math.abs(new HashCodeBuilder(17, 37).append(address.getHostName()).append(address.getHostName()).append(System.getProperty(CLUSTER_NODE_ID_PROPERTY, "0")).toHashCode()));
            }
            catch (UnknownHostException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    public static String createUniqueIdForResource(final MuleContext muleContext, String resourceId)
    {
        String uniqueResourceId = createUniqueIdForServer() + "-" + muleContext.getConfiguration().getId() + "-" + resourceId;
        logger.info("Creating bitronix xa resource with id: " + uniqueResourceId);
        return uniqueResourceId;
    }

    public static int getTransactionRecoveryIntervalInSeconds()
    {
        return Integer.valueOf(System.getProperty(BITRONIX_RECOVERY_INTERVAL, "60"));
    }

    public static String getLogPart2Filename()
    {
        return getLogPartFilename(TransactionManagerServices.getConfiguration().getLogPart2Filename());
    }

    public static String getLogPart1Filename()
    {
        return getLogPartFilename(TransactionManagerServices.getConfiguration().getLogPart1Filename());
    }

    private static String getLogPartFilename(String filename)
    {
        String muleHome = System.getProperty(MuleProperties.MULE_HOME_DIRECTORY_PROPERTY, ".");
        String containerWorkingDirectory = muleHome + "/.mule";
        return containerWorkingDirectory + File.separator + "tx-log" + File.separator + filename;
    }

    public static int getTransactionTimeout()
    {
        return Integer.valueOf(System.getProperty(BITRONIX_TRANSACTION_TIMEOUT, "60"));
    }
}

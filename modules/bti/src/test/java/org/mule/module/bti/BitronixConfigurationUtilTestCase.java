/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.bti;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.module.bti.BitronixConfigurationUtil.BITRONIX_RECOVERY_INTERVAL;
import static org.mule.module.bti.BitronixConfigurationUtil.BITRONIX_TRANSACTION_TIMEOUT;
import static org.mule.module.bti.BitronixConfigurationUtil.CLUSTER_NODE_ID_PROPERTY;
import static org.mule.module.bti.BitronixConfigurationUtil.createUniqueIdForServer;
import static org.mule.module.bti.BitronixConfigurationUtil.getTransactionRecoveryIntervalInSeconds;
import static org.mule.module.bti.BitronixConfigurationUtil.getTransactionTimeout;

import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

public class BitronixConfigurationUtilTestCase extends AbstractMuleTestCase
{

    public static final String UNIQUE_SERVER_ID = "uniqueId";

    @Test
    public void useSystemPropertyForServerId()
    {
        System.setProperty(BitronixConfigurationUtil.BITRONIX_SERVER_ID, UNIQUE_SERVER_ID);
        try
        {
            assertThat(createUniqueIdForServer(), is(UNIQUE_SERVER_ID));
        }
        finally
        {
            System.clearProperty(BitronixConfigurationUtil.BITRONIX_SERVER_ID);
        }
    }

    @Test
    public void createDifferentServerIdWithDifferentNodeIds()
    {
        String serverIdWithNodeOne = createServerIdForNode(1);
        String serverIdWithNodeTwo = createServerIdForNode(2);
        assertThat(serverIdWithNodeOne.equals(serverIdWithNodeTwo), is(false));
    }

    @Test
    public void defaultXaRecoveryInterval()
    {
        assertThat(getTransactionRecoveryIntervalInSeconds(), is(60));
    }

    @Test
    public void xaRecoveryIntervalConfiguredUsingSystemProperty()
    {
        System.setProperty(BITRONIX_RECOVERY_INTERVAL, "20");
        try
        {
            assertThat(getTransactionRecoveryIntervalInSeconds(), is(20));
        }
        finally
        {
            System.clearProperty(BITRONIX_RECOVERY_INTERVAL);
        }
    }

    @Test
    public void defaultTransactionTimeout()
    {
        assertThat(getTransactionTimeout(), is(60));
    }

    @Test
    public void transactionTimeoutUsingSystemProperty()
    {
        System.setProperty(BITRONIX_TRANSACTION_TIMEOUT, "20");
        try
        {
            assertThat(getTransactionTimeout(), is(20));
        }
        finally
        {
            System.clearProperty(BITRONIX_TRANSACTION_TIMEOUT);
        }
    }

    private String createServerIdForNode(int nodeId)
    {
        System.setProperty(CLUSTER_NODE_ID_PROPERTY, String.valueOf(nodeId));
        try
        {
            return createUniqueIdForServer();
        }
        finally
        {
            System.clearProperty(CLUSTER_NODE_ID_PROPERTY);
        }
    }

}

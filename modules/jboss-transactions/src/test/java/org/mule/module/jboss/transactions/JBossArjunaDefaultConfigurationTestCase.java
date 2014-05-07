/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jboss.transactions;

import org.junit.Test;

public class JBossArjunaDefaultConfigurationTestCase extends AbstractJbossArjunaConfigurationTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "jbossts-default-configuration.xml";
    }

    @Test
    public void testConfiguration()
    {
        assertTransactionManagerPresent();
        assertObjectStoreDir(muleContext.getConfiguration().getWorkingDirectory() + "/transaction-log", muleContext.getConfiguration().getWorkingDirectory());
    }

}

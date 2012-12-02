/*
 * $Id:JBossArjunaConfigurationTestCase.java 8215 2012-10-25 15:56:51Z chrismordue $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.jboss.transactions;

import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.transaction.TransactionManagerFactory;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.module.jboss.transaction.JBossArjunaTransactionManagerFactory;
import org.mule.tck.AbstractTxThreadAssociationTestCase;

import org.junit.Test;

import com.arjuna.ats.arjuna.common.arjPropertyManager;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JBossArjunaConfigurationTestCase extends AbstractTxThreadAssociationTestCase
{


    @Override
    protected ConfigurationBuilder getBuilder() throws Exception
    {
        return new SpringXmlConfigurationBuilder(getConfigResources());
    }

    protected String getConfigResources()
    {
        return "jbossts-configuration.xml";
    }

    @Test
    public void testConfiguration()
    {
        assertNotNull(muleContext.getTransactionManager());
        assertTrue(muleContext.getTransactionManager().getClass().getName().compareTo("arjuna") > 0);
        
        assertTrue(arjPropertyManager.getCoordinatorEnvironmentBean().getTxReaperTimeout() == 108000);
        assertTrue(arjPropertyManager.getCoordinatorEnvironmentBean().getDefaultTimeout() == 47);
    }

	@Override
	protected TransactionManagerFactory getTransactionManagerFactory()
	{
		return new JBossArjunaTransactionManagerFactory();
	}

}

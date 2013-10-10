/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

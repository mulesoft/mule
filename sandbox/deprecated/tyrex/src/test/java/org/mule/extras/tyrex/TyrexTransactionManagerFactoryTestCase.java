/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.extras.tyrex;

import org.mule.tck.AbstractMuleTestCase;
import org.mule.util.ClassHelper;

import javax.transaction.TransactionManager;
import java.io.InputStream;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class TyrexTransactionManagerFactoryTestCase extends AbstractMuleTestCase
{
    public void testCreateTransactionManager() throws Exception
    {
        TyrexTransactionManagerFactory factory = new TyrexTransactionManagerFactory();
        factory.setName("TestManager");
        factory.setTimeout(1001);
        factory.setMaximumTransactions(10);

        TransactionManager txManager = factory.create();

        assertNotNull(txManager);

        assertEquals(10, factory.getMaximumTransactions());
        assertEquals(1001, factory.getTimeout());
        assertEquals("TestManager", factory.getName());

        assertEquals("TestManager", factory.getTransactionDomain().getDomainName());

    }

    public void testCreateTransactionManagerFromDomainconfigFile() throws Exception
    {
        TyrexTransactionManagerFactory factory = new TyrexTransactionManagerFactory();
        InputStream is = ClassHelper.getResourceAsStream("tyrex-domain.xml", getClass());
        factory.setDomainConfig(is);

        TransactionManager txManager = factory.create();

        assertNotNull(txManager);
        assertNotNull(factory.getTransactionDomain());

        assertEquals("TestManagerFromFile", factory.getTransactionDomain().getDomainName());
    }
}

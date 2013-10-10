/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.jboss.transactions;

import org.mule.api.transaction.TransactionManagerFactory;
import org.mule.module.jboss.transaction.JBossArjunaTransactionManagerFactory;
import org.mule.tck.AbstractTxThreadAssociationTestCase;

public class JBossArjunaTxThreadAssociationTestCase extends AbstractTxThreadAssociationTestCase
{

    protected TransactionManagerFactory getTransactionManagerFactory()
    {
        return new JBossArjunaTransactionManagerFactory();
    }
}

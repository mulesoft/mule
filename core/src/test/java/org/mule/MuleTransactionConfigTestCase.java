/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule;

import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transaction.TransactionException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.mule.TestTransactionFactory;
import org.mule.transaction.MuleTransactionConfig;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class MuleTransactionConfigTestCase extends AbstractMuleContextTestCase
{
    @Test
    public void testActionAndStringConversion()
    {
        MuleTransactionConfig c = new MuleTransactionConfig();
        c.setMuleContext(muleContext);

        c.setAction(MuleTransactionConfig.ACTION_ALWAYS_BEGIN);
        assertEquals(MuleTransactionConfig.ACTION_ALWAYS_BEGIN_STRING, c.getActionAsString());

        c.setAction(MuleTransactionConfig.ACTION_ALWAYS_JOIN);
        assertEquals(MuleTransactionConfig.ACTION_ALWAYS_JOIN_STRING, c.getActionAsString());

        c.setAction(MuleTransactionConfig.ACTION_BEGIN_OR_JOIN);
        assertEquals(MuleTransactionConfig.ACTION_BEGIN_OR_JOIN_STRING, c.getActionAsString());

        c.setAction(MuleTransactionConfig.ACTION_JOIN_IF_POSSIBLE);
        assertEquals(MuleTransactionConfig.ACTION_JOIN_IF_POSSIBLE_STRING, c.getActionAsString());

        c.setAction(MuleTransactionConfig.ACTION_NONE);
        assertEquals(MuleTransactionConfig.ACTION_NONE_STRING, c.getActionAsString());

        c.setAction(MuleTransactionConfig.ACTION_INDIFFERENT);
        assertEquals(MuleTransactionConfig.ACTION_INDIFFERENT_STRING, c.getActionAsString());
    }

    @Test
    public void testDefaults() throws Exception {
        MuleTransactionConfig c = new MuleTransactionConfig(TransactionConfig.ACTION_ALWAYS_BEGIN);
        c.setMuleContext(muleContext);
        assertEquals("Wrong default TX timeout", 30000, c.getTimeout());
    }

    @Test
    public void testTransactionJoinIfPossible() throws TransactionException
    {      
        MuleTransactionConfig txConfig = new MuleTransactionConfig(TransactionConfig.ACTION_JOIN_IF_POSSIBLE);
        txConfig.setMuleContext(muleContext);
        txConfig.setFactory(new TestTransactionFactory());
        assertFalse(txConfig.isTransacted());
    }

    @Test
    public void testFailNoFactory()
    {
        MuleTransactionConfig txConfig = new MuleTransactionConfig(TransactionConfig.ACTION_ALWAYS_BEGIN);
        txConfig.setMuleContext(muleContext);
        // note how we don't set a factory here so the default in MTC is null
        
        try
        {
            txConfig.isTransacted();
            fail("isTransacted() must fail if no factory is set");
        }
        catch (RuntimeException re)
        {
            // this was expected
        }
    }

}

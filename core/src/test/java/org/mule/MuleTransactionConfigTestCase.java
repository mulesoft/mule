/*
 * $Id:MuleTransactionConfigTestCase.java 7383 2007-07-07 22:21:30Z aperepel $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule;

import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transaction.TransactionException;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.mule.TestTransactionFactory;
import org.mule.transaction.MuleTransactionConfig;

public class MuleTransactionConfigTestCase extends AbstractMuleTestCase
{
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
    }

    public void testDefaults() throws Exception {
        MuleTransactionConfig c = new MuleTransactionConfig();
        c.setMuleContext(muleContext);
        assertEquals("Wrong default TX timeout", 30000, c.getTimeout());
    }

    public void testTransactionJoinIfPossible() throws TransactionException
    {      
        MuleTransactionConfig txConfig = new MuleTransactionConfig();
        txConfig.setMuleContext(muleContext);
        txConfig.setAction(TransactionConfig.ACTION_JOIN_IF_POSSIBLE);
        txConfig.setFactory(new TestTransactionFactory());
        assertFalse(txConfig.isTransacted());
    }

    public void testFailNoFactory()
    {
        MuleTransactionConfig txConfig = new MuleTransactionConfig();
        txConfig.setMuleContext(muleContext);
        txConfig.setAction(TransactionConfig.ACTION_ALWAYS_BEGIN);
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

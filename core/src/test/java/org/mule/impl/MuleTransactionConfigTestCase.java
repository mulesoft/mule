/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl;

import org.mule.tck.AbstractMuleTestCase;

public class MuleTransactionConfigTestCase extends AbstractMuleTestCase
{
    public void testActionAndStringConversion()
    {
        MuleTransactionConfig c = new MuleTransactionConfig();

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
}

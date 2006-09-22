/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transaction.lookup;

/**
 * @author <a href="mailto:aperepel@gmail.com">Andrew Perepelytsya</a>
 */
public class JRunTransactionManagerLookupFactory extends GenericTransactionManagerLookupFactory
{
    public JRunTransactionManagerLookupFactory()
    {
        setJndiName("java:/TransactionManager");
    }
}

/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transaction.lookup;

public class Resin3TransactionManagerLookupFactory extends GenericTransactionManagerLookupFactory
{
    public Resin3TransactionManagerLookupFactory()
    {
        setJndiName("java:comp/TransactionManager");
    }
}

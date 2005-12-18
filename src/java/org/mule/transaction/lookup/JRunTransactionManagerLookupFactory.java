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
 *
 */
package org.mule.transaction.lookup;

/**
 * @author <a href="mailto:aperepel@itci.com">Andrew Perepelytsya</a>
 */
public class JRunTransactionManagerLookupFactory extends GenericTransactionManagerLookupFactory
{
    public JRunTransactionManagerLookupFactory()
    {
        setJndiName("java:/TransactionManager");
    }
}

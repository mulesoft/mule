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
package org.mule.tck.testmodels.mule;

import com.mockobjects.dynamic.Mock;

import org.mule.umo.TransactionException;
import org.mule.umo.UMOTransaction;
import org.mule.umo.UMOTransactionFactory;

/**
 * <code>TestTransactionFactory</code> TODO
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class TestTransactionFactory implements UMOTransactionFactory
{
    public UMOTransaction beginTransaction() throws TransactionException
    {
        return (UMOTransaction) new Mock(UMOTransaction.class).proxy();
    }

    public boolean isTransacted()
    {
        return true;
    }
}

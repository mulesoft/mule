/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.tck.testmodels.mule;

import com.mockobjects.dynamic.Mock;
import org.mule.umo.UMOTransaction;
import org.mule.umo.UMOTransactionException;
import org.mule.umo.UMOTransactionFactory;

/**
 * <code>TestTransactionFactory</code> TODO
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public class TestTransactionFactory implements UMOTransactionFactory
{
    public UMOTransaction beginTransaction() throws UMOTransactionException
    {
        return (UMOTransaction)new Mock(UMOTransaction.class).proxy();
    }

    public boolean isTransacted()
    {
        return true;
    }
}

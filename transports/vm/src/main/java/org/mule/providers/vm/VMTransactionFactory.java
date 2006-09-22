/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.vm;

import org.mule.umo.TransactionException;
import org.mule.umo.UMOTransaction;
import org.mule.umo.UMOTransactionFactory;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class VMTransactionFactory implements UMOTransactionFactory
{

    public UMOTransaction beginTransaction() throws TransactionException
    {
        VMTransaction tx = new VMTransaction();
        tx.begin();
        return tx;
    }

    public boolean isTransacted()
    {
        return true;
    }

}

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
package org.mule.test.transaction;

import com.mockobjects.dynamic.Mock;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.transaction.TransactionCoordination;
import org.mule.transaction.TransactionProxy;
import org.mule.transaction.constraints.ConstraintFilter;
import org.mule.umo.UMOTransaction;

/**
 * <code>TransactionCoordinationTestCase</code> TODO
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public class TransactionCoordinationTestCase extends AbstractMuleTestCase
{
    TransactionCoordination tc;

    protected void setUp() throws Exception
    {
        super.setUp();
        Object x = TransactionCoordination.getInstance().unbindTransaction();
        TransactionCoordination.setInstance(null);
        tc = TransactionCoordination.getInstance();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
        TransactionCoordination.setInstance(null);
    }

    public void testTransactionCoordination() throws Exception
    {
        assertNull(tc.getTransaction());
        Mock trans = new Mock(UMOTransaction.class, "trans");
        TransactionProxy tp = new TransactionProxy((UMOTransaction)trans.proxy(), null);

        trans.expectAndReturn("getResource", new String("dummy resource"));

        tc.bindTransaction(tp);
        UMOTransaction tx = tc.getTransaction();
        assertNotNull(tx);

        tx = null;
        tx = tc.getTransactionProxy();
        assertNotNull(tx);
        Object resource = tc.getTransactionSession();
        assertNotNull(resource);
        assertEquals(resource, "dummy resource");

        tx = tc.unbindTransaction();
        assertNotNull(tx);
        assertNull(tc.getTransaction());
        assertNull(tc.getTransactionProxy());

        tc.bindTransaction((UMOTransaction)trans.proxy(), new ConstraintFilter());
        tp = tc.getTransactionProxy();
        assertNotNull(tp);
        assertNotNull(tp.getConstraint());

        try
        {
            TransactionCoordination.setInstance(null);
            fail("shouldn't be able to replace the tc if there are transactions in progress");
        } catch (IllegalStateException e)
        {
            //expected
        }

        tc.unbindTransaction();
        TransactionCoordination.setInstance(null);
    }
}

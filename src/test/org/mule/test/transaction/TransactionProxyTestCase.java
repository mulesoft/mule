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
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOTransaction;
import org.mule.umo.UMOTransactionException;

/**
 * <code>TransactionProxyTestCase</code> TODO
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public class TransactionProxyTestCase extends AbstractMuleTestCase
{

    public void testBeginCommit() throws Exception
    {
        Mock trans = getMockTransaction();
        UMOEvent event = getTestEvent("Test");

        trans.expect("begin");
        trans.expect("commit");
        trans.expectAndReturn("isBegun", true);
        trans.expectAndReturn("isCommitted", false);
        trans.expectAndReturn("isCommitted", true);
        trans.expectAndReturn("isRollbackOnly", false);
        trans.expectAndReturn("isRollbackOnly", false);
        trans.expectAndReturn("isRollbackOnly", false);

        TransactionProxy tp = new TransactionProxy((UMOTransaction)trans.proxy(), null);

        tp.begin();
        assertTrue(tp.isBegun());
        assertTrue(!tp.isCommitted());

        assertNull(tp.getConstraint());
        assertTrue(tp.canCommit(event));
        assertNotNull(tp.getTransaction());

        try
        {
            tp.commit();
            fail("When using a proxy only commit(event) method can be called");
        } catch (UMOTransactionException e)
        {
            //expected
        }

        tp.commit(event);

        assertTrue(tp.isCommitted());

        trans.verify();
    }

    public void testBeginRollback() throws Exception
    {
        Mock trans = getMockTransaction();
        UMOEvent event = getTestEvent("Test");

        trans.expect("begin");
        trans.expectAndReturn("isBegun", true);
        trans.expect("setRollbackOnly");
        trans.expectAndReturn("isRollbackOnly", true);
        trans.expectAndReturn("isRollbackOnly", true);

        trans.expect("rollback");
        trans.expectAndReturn("isRolledBack", true);
        trans.expectAndReturn("isCommitted", false);

        TransactionProxy tp = new TransactionProxy((UMOTransaction)trans.proxy(), null);

        tp.begin();
        assertTrue(tp.isBegun());
        tp.setRollbackOnly();
        assertTrue(tp.isRollbackOnly());
        try
        {
            tp.commit(event);
            fail("Cant commit when rollback only is set");
        } catch (UMOTransactionException e)
        {
            //expected
        }

        tp.rollback();
        assertTrue(tp.isRolledBack());
        assertTrue(!tp.isCommitted());
        trans.verify();
    }

     public void testProperties() throws Exception
    {
        Mock trans = getMockTransaction();
        trans.expectAndReturn("getResource", new String("Test Resource"));
        trans.expectAndReturn("getStatus", 1);

        TransactionProxy tp = new TransactionProxy((UMOTransaction)trans.proxy(), new ConstraintFilter());
        assertNotNull(tp.getTransaction());
        Object resource = tp.getResource();
        assertEquals(resource, "Test Resource");
        assertNotNull(tp.getConstraint());
        assertEquals(tp.getStatus(), 1);
        tp.setTransaction(null);
        assertNull(tp.getTransaction());

        trans.verify();
    }

    public void testCleanup() throws Exception
    {
        assertNull("There should be no transaction associated with this thread",TransactionCoordination.getInstance().unbindTransaction());
    }
}

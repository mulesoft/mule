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

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;
import org.mule.MuleManager;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.transaction.XaTransaction;
import org.mule.transaction.XaTransactionFactory;
import org.mule.umo.UMOTransactionException;

import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;

/**
 * <code>XATransactionTestCase</code> TODO
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public class XaTransactionTestCase extends AbstractMuleTestCase
{
    protected void setUp() throws Exception
    {
        super.setUp();
        if(MuleManager.isInstanciated()) MuleManager.getInstance().dispose();
    }

    public void testBeginCommit() throws Exception
    {
        Mock trans = new Mock(Transaction.class);
        XaTransaction xatx = new XaTransaction((Transaction)trans.proxy());
        Mock resource = new Mock(XAResource.class);

        trans.expect("commit");
        trans.expectAndReturn("enlistResource", C.eq((XAResource)resource.proxy()), true);
        trans.expectAndReturn("delistResource", C.args(C.eq((XAResource)resource.proxy()), C.eq(XAResource.TMSUCCESS)), true);

        assertTrue(xatx.enlistResource((XAResource)resource.proxy()));


        xatx.begin();
        assertTrue(xatx.isBegun());
        assertTrue(!xatx.isCommitted());
        xatx.delistResource((XAResource)resource.proxy(), XAResource.TMSUCCESS);
        xatx.commit();

        assertTrue(xatx.isCommitted());
        resource.verify();
        trans.verify();
    }

       public void testBeginRollback() throws Exception
    {
        Mock trans = new Mock(Transaction.class);
        XaTransaction xatx = new XaTransaction((Transaction)trans.proxy());
        Mock resource = new Mock(XAResource.class);

        trans.expectAndReturn("enlistResource", C.eq((XAResource)resource.proxy()), true);

        assertTrue(xatx.enlistResource((XAResource)resource.proxy()));
        trans.expect("rollback");

        xatx.begin();
        assertTrue(xatx.isBegun());
        xatx.setRollbackOnly();
        assertTrue(xatx.isRollbackOnly());
        try
        {
            xatx.commit();
            fail("Cant commit when rollback only is set");
        } catch (UMOTransactionException e)
        {
            //expected
        }

        xatx.rollback();
        assertTrue(xatx.isRolledBack());
        assertTrue(!xatx.isCommitted());
        trans.verify();
    }

     public void testProperties() throws Exception
    {
        Mock trans = new Mock(Transaction.class);
        XaTransaction xatx = new XaTransaction((Transaction)trans.proxy());

        trans.expectAndReturn("getStatus", 1);

        assertNotNull(xatx.getResource());
        assertEquals(xatx.getStatus(), 1);

        trans.verify();
    }

    public void testTransactionFactoryNoManager() throws Exception
    {
        XaTransactionFactory txFactory = null;

        try
        {
            txFactory = new XaTransactionFactory();
            fail("There is no transaction manager available");
        } catch (IllegalStateException e)
        {
            //expected
        }
    }

    public void testTransactionFactory() throws Exception
    {
//        Mock mockTxManager = new Mock(TransactionManager.class);
//        MuleManager.getInstance().setTransactionManager((TransactionManager)mockTxManager.proxy());
//
//        XaTransactionFactory txFactory = new XaTransactionFactory();
//
//
//        Mock mockXaResource = new Mock(XAResource.class);
//
//        Mock mockTransaction = new Mock(Transaction.class);
//
//        mockTxManager.expectAndReturn("getTransaction", (Transaction)mockTransaction.proxy());
//        mockTransaction.expect("enlistResource", C.eq(mockXaResource.proxy()));
//
//        UMOTransaction xatx = txFactory.beginTransaction(mockXaResource.proxy());
//        assertNull(xatx);
//        assertTrue(xatx instanceof XaTransaction);
//
//        mockTransaction.verify();
//        mockTxManager.verify();
//        mockXaResource.verify();
    }

}

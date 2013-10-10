/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.testmodels.mule;

import org.mule.api.MuleContext;
import org.mule.api.transaction.TransactionException;
import org.mule.transaction.AbstractSingleResourceTransaction;

import javax.transaction.Transaction;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A test transaction that does nothing on commit or rollback. The transaction does retain a status so that
 * developers can determine if the the transaction was rolled back or committed.
 */
public class TestTransaction extends AbstractSingleResourceTransaction
{
    private AtomicBoolean committed = new AtomicBoolean(false);
    private AtomicBoolean rolledBack = new AtomicBoolean(false);

    private String testProperty;
    private boolean isXA;

    public TestTransaction(MuleContext muleContext)
    {
        super(muleContext);
    }

    @Override
    protected Class getResourceType()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected Class getKeyType()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public TestTransaction(MuleContext mockMuleContext, boolean isXa)
    {
        super(mockMuleContext);
        this.isXA = isXa;
    }

    /**
     * Really begin the transaction. Note that resources are enlisted yet.
     *
     * @throws org.mule.api.transaction.TransactionException
     *
     */
    protected void doBegin() throws TransactionException
    {
        //do nothing
    }

    /**
     * Commit the transaction on the underlying resource
     *
     * @throws org.mule.api.transaction.TransactionException
     *
     */
    protected void doCommit() throws TransactionException
    {
        committed.set(true);
    }

    /**
     * Rollback the transaction on the underlying resource
     *
     * @throws org.mule.api.transaction.TransactionException
     *
     */
    protected void doRollback() throws TransactionException
    {
        rolledBack.set(true);
    }

    public String getTestProperty()
    {
        return testProperty;
    }

    public void setTestProperty(String testProperty)
    {
        this.testProperty = testProperty;
    }

    @Override
    public boolean isXA()
    {
        return isXA;
    }


    public void setXA(boolean xa)
    {
        isXA = xa;
    }

    @Override
    public Transaction suspend() throws TransactionException
    {
        if (isXA)
        {
            return null;
        }
        return super.suspend();
    }

    @Override
    public void resume() throws TransactionException
    {
        if (isXA)
        {
            return;
        }
        super.suspend();
    }
}

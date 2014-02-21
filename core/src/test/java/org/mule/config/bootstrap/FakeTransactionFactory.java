package org.mule.config.bootstrap;

import org.mule.api.MuleContext;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionException;
import org.mule.api.transaction.UniversalTransactionFactory;

public final class FakeTransactionFactory implements UniversalTransactionFactory
    {

        @Override
        public Transaction beginTransaction(MuleContext muleContext)
                throws TransactionException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean isTransacted() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public Transaction createUnboundTransaction(MuleContext muleContext)
                throws TransactionException {
            // TODO Auto-generated method stub
            return null;
        }
        
    }
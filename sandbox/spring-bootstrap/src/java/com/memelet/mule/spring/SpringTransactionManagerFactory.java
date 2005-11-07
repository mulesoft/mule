package com.memelet.mule.spring;

import javax.transaction.TransactionManager;

import org.mule.umo.manager.UMOTransactionManagerFactory;

public class SpringTransactionManagerFactory implements UMOTransactionManagerFactory {

    private TransactionManager transactionManager;
    
    public void setTransactionManager(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }
    
    public TransactionManager create() throws Exception {
        return transactionManager;
    }

}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.spring.transaction;

import org.mule.api.transaction.Transaction;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transaction.TransactionCoordination;

import org.junit.Test;

public class SpringTransactionFactoryTestCase extends AbstractMuleContextTestCase
{

    @Override
    protected void doTearDown() throws Exception
    {
        Transaction tx = TransactionCoordination.getInstance().getTransaction();
        if (tx != null)
        {
            TransactionCoordination.getInstance().unbindTransaction(tx);
        }
    }

    @Test
    public void testTransactionFactoryBinding() throws Exception
    {
        // Init a data source
//        DriverManagerDataSource ds = new DriverManagerDataSource();
//        ds.setDriverClassName("org.hsqldb.jdbcDriver");
//        ds.setUrl("jdbc:hsqldb:mem:db");
//        ds.setUsername("sa");
//        // Init hibernate
//        LocalSessionFactoryBean sfactory = new LocalSessionFactoryBean();
//        sfactory.setDataSource(ds);
//        sfactory.afterPropertiesSet();
//        // Init hibernate transaction manager
//        HibernateTransactionManager tm = new HibernateTransactionManager();
//        tm.setDataSource(ds);
//        SessionFactory sessionFactory = (SessionFactory)sfactory.getObject();
//        tm.setSessionFactory(sessionFactory);
//        // Init spring transaction factory
//        SpringTransactionFactory factory = new SpringTransactionFactory();
//        factory.setManager(tm);
//
//        // Create a new transaction
//        Transaction tx = factory.beginTransaction();
//        TransactionCoordination.getInstance().bindTransaction(tx);
//        // Check that the jdbc connection is enlisted
//        assertTrue(tx.hasResource(ds));
//        // Check that the hibernate session is enlisted
//        assertTrue(tx.hasResource(sessionFactory));

     //   TransactionCoordination.getInstance().unbindTransaction(tx);
    }

}

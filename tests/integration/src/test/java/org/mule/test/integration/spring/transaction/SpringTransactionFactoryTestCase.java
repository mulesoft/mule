/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.spring.transaction;

import org.mule.extras.spring.transaction.SpringTransactionFactory;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.transaction.TransactionCoordination;
import org.mule.umo.UMOTransaction;

import org.hibernate.SessionFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.hibernate3.HibernateTransactionManager;
import org.springframework.orm.hibernate3.LocalSessionFactoryBean;

public class SpringTransactionFactoryTestCase extends AbstractMuleTestCase
{

    // @Override
    protected void doTearDown() throws Exception
    {
        UMOTransaction tx = TransactionCoordination.getInstance().getTransaction();
        if (tx != null)
        {
            TransactionCoordination.getInstance().unbindTransaction(tx);
        }
    }

    public void testTransactionFactoryBinding() throws Exception
    {
        // Init a data source
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.hsqldb.jdbcDriver");
        ds.setUrl("jdbc:hsqldb:mem:db");
        ds.setUsername("sa");
        // Init hibernate
        LocalSessionFactoryBean sfactory = new LocalSessionFactoryBean();
        sfactory.setDataSource(ds);
        sfactory.afterPropertiesSet();
        // Init hibernate transaction manager
        HibernateTransactionManager tm = new HibernateTransactionManager();
        tm.setDataSource(ds);
        SessionFactory sessionFactory = (SessionFactory)sfactory.getObject();
        tm.setSessionFactory(sessionFactory);
        // Init spring transaction factory
        SpringTransactionFactory factory = new SpringTransactionFactory();
        factory.setManager(tm);

        // Create a new transaction
        UMOTransaction tx = factory.beginTransaction();
        TransactionCoordination.getInstance().bindTransaction(tx);
        // Check that the jdbc connection is enlisted
        assertTrue(tx.hasResource(ds));
        // Check that the hibernate session is enlisted
        assertTrue(tx.hasResource(sessionFactory));

        TransactionCoordination.getInstance().unbindTransaction(tx);
    }

}

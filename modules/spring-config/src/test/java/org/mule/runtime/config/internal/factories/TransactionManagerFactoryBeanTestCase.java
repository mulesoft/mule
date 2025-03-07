/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.factories;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;

import org.mule.runtime.core.api.context.DefaultMuleContextFactory;
import org.mule.runtime.core.internal.config.builders.MinimalConfigurationBuilder;
import org.mule.runtime.core.internal.context.DefaultMuleContext;
import org.mule.tck.config.TestServicesConfigurationBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.testmodels.mule.TestTransactionManagerFactory;

import org.junit.Test;

import jakarta.transaction.TransactionManager;

public class TransactionManagerFactoryBeanTestCase extends AbstractMuleTestCase {

  @Test
  public void registerTransactionManager() throws Exception {
    DefaultMuleContext context =
        (DefaultMuleContext) new DefaultMuleContextFactory().createMuleContext(new TestServicesConfigurationBuilder(),
                                                                               new MinimalConfigurationBuilder());

    TransactionManagerFactoryBean txMgrFB = new TransactionManagerFactoryBean();
    txMgrFB.setMuleContext(context);
    txMgrFB.setTxManagerFactory(new TestTransactionManagerFactory());
    TransactionManager transactionManager = txMgrFB.getObject();
    assertThat(transactionManager, not(is(nullValue())));
  }

}

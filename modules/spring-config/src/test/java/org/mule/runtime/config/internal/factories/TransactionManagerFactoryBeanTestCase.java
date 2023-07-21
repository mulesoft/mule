/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.factories;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.internal.config.builders.MinimalConfigurationBuilder;
import org.mule.runtime.core.api.context.DefaultMuleContextFactory;
import org.mule.runtime.core.internal.context.DefaultMuleContext;
import org.mule.tck.config.TestServicesConfigurationBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.testmodels.mule.TestTransactionManagerFactory;

import javax.transaction.TransactionManager;

import org.hamcrest.core.IsNull;
import org.junit.Test;

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
    assertThat(transactionManager, not(is(IsNull.nullValue())));
  }

}

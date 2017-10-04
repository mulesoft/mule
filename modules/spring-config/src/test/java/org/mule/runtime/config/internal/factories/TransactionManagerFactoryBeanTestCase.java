/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.factories;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.internal.config.builders.DefaultsConfigurationBuilder;
import org.mule.runtime.core.internal.context.DefaultMuleContext;
import org.mule.runtime.config.internal.factories.TransactionManagerFactoryBean;
import org.mule.runtime.core.api.context.DefaultMuleContextFactory;
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
                                                                               new DefaultsConfigurationBuilder());

    TransactionManagerFactoryBean txMgrFB = new TransactionManagerFactoryBean();
    txMgrFB.setMuleContext(context);
    txMgrFB.setTxManagerFactory(new TestTransactionManagerFactory());
    TransactionManager transactionManager = txMgrFB.getObject();
    assertThat(transactionManager, not(is(IsNull.nullValue())));
  }

}

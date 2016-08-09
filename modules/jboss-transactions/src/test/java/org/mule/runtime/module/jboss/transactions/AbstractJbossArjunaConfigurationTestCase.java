/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.jboss.transactions;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.module.jboss.transaction.JBossArjunaTransactionManagerFactory.OS_ROOT;
import static org.mule.runtime.module.jboss.transaction.JBossArjunaTransactionManagerFactory.PROPERTY_ENVIRONMENT_OBJECTSTORE_DIR;
import static org.mule.runtime.module.jboss.transaction.JBossArjunaTransactionManagerFactory.PROPERTY_OBJECTSTORE_DIR;
import static org.mule.runtime.module.jboss.transaction.JBossArjunaTransactionManagerFactory.PROPERTY_USER_DIR;

import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.transaction.TransactionManagerFactory;
import org.mule.runtime.config.spring.SpringXmlConfigurationBuilder;
import org.mule.runtime.module.jboss.transaction.JBossArjunaTransactionManagerFactory;
import org.mule.tck.AbstractTxThreadAssociationTestCase;

import java.util.Properties;

import com.arjuna.ats.arjuna.common.arjPropertyManager;
import com.arjuna.common.util.propertyservice.PropertiesFactory;

public abstract class AbstractJbossArjunaConfigurationTestCase extends AbstractTxThreadAssociationTestCase {

  protected abstract String getConfigResources();

  @Override
  protected ConfigurationBuilder getBuilder() throws Exception {
    return new SpringXmlConfigurationBuilder(getConfigResources());
  }

  @Override
  protected TransactionManagerFactory getTransactionManagerFactory() {
    return new JBossArjunaTransactionManagerFactory();
  }

  protected void assertTransactionManagerPresent() {
    assertNotNull(muleContext.getTransactionManager());
    assertTrue(muleContext.getTransactionManager().getClass().getName().compareTo("arjuna") > 0);
  }

  protected void assertObjectStoreDir(String objectStoreDir, String workingDirectory) {
    assertThat(objectStoreDir, is(arjPropertyManager.getObjectStoreEnvironmentBean().getObjectStoreDir()));
    assertThat(OS_ROOT, is(arjPropertyManager.getObjectStoreEnvironmentBean().getLocalOSRoot()));

    Properties props = PropertiesFactory.getDefaultProperties();
    assertEquals(props.getProperty(PROPERTY_ENVIRONMENT_OBJECTSTORE_DIR), objectStoreDir);
    assertEquals(props.getProperty(PROPERTY_OBJECTSTORE_DIR), objectStoreDir);
    assertEquals(props.getProperty(PROPERTY_USER_DIR), workingDirectory);
  }

}

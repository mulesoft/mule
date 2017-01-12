/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.domain.xa;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.internal.matchers.ThrowableMessageMatcher.hasMessage;
import static org.mule.functional.junit4.matchers.ThrowableRootCauseMatcher.hasRootCause;

import org.mule.functional.junit4.DomainFunctionalTestCase;
import org.mule.runtime.api.lifecycle.InitialisationException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;

public class XaTransactionManagerTestCase extends DomainFunctionalTestCase {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Override
  public void setUpMuleContexts() throws Exception {
    thrown.expect(InitialisationException.class);
    thrown
        .expect(hasMessage(containsString("No qualifying bean of type [org.mule.runtime.core.api.transaction.TransactionManagerFactory] is defined: expected single matching bean but found 2:")));
    thrown.expect(hasRootCause(instanceOf(NoUniqueBeanDefinitionException.class)));
    super.setUpMuleContexts();
  }

  public static final String APPLICATION_NAME = "app";

  @Override
  protected String getDomainConfig() {
    return "domain/xa/jboss-ts-config.xml";
  }

  @Override
  public ApplicationConfig[] getConfigResources() {
    return new ApplicationConfig[] {
        new ApplicationConfig(APPLICATION_NAME, new String[] {"domain/xa/app-with-tx-manager-config.xml"})};
  }

  @Test
  public void validateOnlyOneTxManagerCanBeUsed() {
    // This is never called since the exception is thrown during init.
    getMuleContextForApp(APPLICATION_NAME).getTransactionManager();
  }
}

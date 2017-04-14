/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.transaction;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mule.runtime.extension.api.tx.SourceTransactionalAction.ALWAYS_BEGIN;
import static org.mule.runtime.extension.api.tx.SourceTransactionalAction.NONE;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.extension.api.tx.SourceTransactionalAction;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.runner.RunnerDelegateTo;
import org.mule.test.transactional.TransactionalSourceWithTXParameters;

import org.junit.Test;
import org.junit.runners.Parameterized;

import java.util.Collection;

@RunnerDelegateTo(Parameterized.class)
public class TransactionalActionParameterTestCase extends AbstractExtensionFunctionalTestCase {

  @Parameterized.Parameter
  public String flowName;

  @Parameterized.Parameter(1)
  public SourceTransactionalAction transactionalAction;

  @Parameterized.Parameters(name = "{0} - Excepted: {1}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {"alwaysBeginTxAction", ALWAYS_BEGIN},
        {"defaultTxAction", NONE}
    });
  }

  @Override
  protected String getConfigFile() {
    return "transactional-parameters-injection-config.xml";
  }

  @Test
  public void injectSourceTransactionalAction() throws Exception {
    Reference<SourceTransactionalAction> sourceTransactionalAction = new Reference<>();
    TransactionalSourceWithTXParameters.responseCallback = tx -> sourceTransactionalAction.set((SourceTransactionalAction) tx);
    startFlow(flowName);

    assertThat(sourceTransactionalAction.get(), is(transactionalAction));
  }

  private void startFlow(String flowName) throws Exception {
    ((Flow) getFlowConstruct(flowName)).start();
  }
}

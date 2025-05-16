/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.transaction;

import static org.mule.runtime.extension.api.tx.OperationTransactionalAction.JOIN_IF_POSSIBLE;
import static org.mule.runtime.extension.api.tx.OperationTransactionalAction.NOT_SUPPORTED;
import static org.mule.runtime.extension.api.tx.SourceTransactionalAction.ALWAYS_BEGIN;

import static java.util.Arrays.asList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.extension.api.tx.SourceTransactionalAction;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.runner.RunnerDelegateTo;
import org.mule.test.transactional.TransactionalSourceWithTXParameters;

import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
public class TransactionalActionParameterTestCase extends AbstractExtensionFunctionalTestCase {

  private boolean isSdkApi;
  private final String configFile;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> configs() {
    return asList(new Object[][] {
        {"tx/transactional-parameters-injection-config.xml", false},
        {"tx/transactional-xa-parameters-injection-config.xml", true}
    });
  }

  public TransactionalActionParameterTestCase(String configFile, boolean isSdkApi) {
    this.isSdkApi = isSdkApi;
    this.configFile = configFile;
  }

  @Override
  protected String getConfigFile() {
    return configFile;
  }

  @Test
  public void injectAlwaysBeginSourceTransactionalAction() throws Exception {
    Reference<org.mule.sdk.api.tx.SourceTransactionalAction> sdkSourceTransactionalAction = new Reference<>();
    Reference<SourceTransactionalAction> sourceTransactionalAction = new Reference<>();

    if (isSdkApi) {
      org.mule.test.transactionalxa.TransactionalSourceWithTXParameters.responseCallback =
          tx -> sdkSourceTransactionalAction.set((org.mule.sdk.api.tx.SourceTransactionalAction) tx);
    } else {
      TransactionalSourceWithTXParameters.responseCallback =
          tx -> sourceTransactionalAction.set((SourceTransactionalAction) tx);
    }

    startFlow("alwaysBeginTxAction");

    if (isSdkApi) {
      assertThat(sdkSourceTransactionalAction.get(), is(org.mule.sdk.api.tx.SourceTransactionalAction.ALWAYS_BEGIN));
    } else {
      assertThat(sourceTransactionalAction.get(), is(ALWAYS_BEGIN));
    }
  }

  @Test
  public void injectDefaultOperationTransactionalAction() throws Exception {
    Enum value = (Enum) flowRunner("injectInOperationDefaultValue").run()
        .getMessage().getPayload().getValue();
    assertThat(value.name(), is(JOIN_IF_POSSIBLE.name()));
  }

  @Test
  public void injectInOperationJoinNotSupported() throws Exception {
    Enum value = (Enum) flowRunner("injectInOperationJoinNotSupported").run()
        .getMessage().getPayload().getValue();
    assertThat(value.name(), is(NOT_SUPPORTED.name()));
  }

  private void startFlow(String flowName) throws Exception {
    ((Flow) getFlowConstruct(flowName)).start();
  }
}

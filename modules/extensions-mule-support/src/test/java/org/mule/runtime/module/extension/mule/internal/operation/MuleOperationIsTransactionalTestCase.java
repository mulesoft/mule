/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.operation;

import static org.mule.runtime.extension.api.ExtensionConstants.TRANSACTIONAL_ACTION_PARAMETER_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.extension.ExtensionManager;

import org.junit.Test;
import javax.inject.Inject;
import java.util.List;

public class MuleOperationIsTransactionalTestCase extends MuleArtifactFunctionalTestCase {

  @Inject
  private ExtensionManager extensionManager;

  private static List<OperationModel> operationModels;

  @Override
  protected String getConfigFile() {
    return "mule-tx-ops-config.xml";
  }

  @Test
  public void withoutInnerTransactionalOps() {
    assertForOpeartion("withoutTxAction", false);
  }

  @Test
  public void withTxActionNotJoining() {
    assertForOpeartion("withTxActionNotJoining", false);
  }

  @Test
  public void withTxActionJoining() {
    assertForOpeartion("withTxActionJoining", true);
  }

  @Test
  public void withTxActionAlwaysJoining() {
    assertForOpeartion("withTxActionAlwaysJoining", true);
  }

  @Test
  public void withTxActionJoiningWithinTryIndifferent() {
    assertForOpeartion("withTxActionJoiningWithinTryIndifferent", true);
  }

  @Test
  public void withTxActionNotJoiningWithinTryIndifferent() {
    assertForOpeartion("withTxActionNotJoiningWithinTryIndifferent", false);
  }

  @Test
  public void withTxActionAlwaysJoiningWithinTryJoining() {
    assertForOpeartion("withTxActionAlwaysJoiningWithinTryJoining", true);
  }

  @Test
  public void withTxActionNotJoiningWithinTryJoining() {
    assertForOpeartion("withTxActionNotJoiningWithinTryJoining", false);
  }

  @Test
  public void withTxActionAlwaysJoiningWithinTryCreateTx() {
    assertForOpeartion("withTxActionAlwaysJoiningWithinTryCreateTx", false);
  }

  @Test
  public void withTxActionAlwaysNotJoiningWithinTryCreateTx() {
    assertForOpeartion("withTxActionAlwaysNotJoiningWithinTryCreateTx", false);
  }

  @Test
  public void withTxActionAlwaysJoiningWithinTryCreateTxFollowed() {
    assertForOpeartion("withTxActionAlwaysJoiningWithinTryCreateTxFollowed", true);
  }

  @Test
  public void withTxActionAlwaysJoiningWithinTryCreateTxFollowedNot() {
    assertForOpeartion("withTxActionAlwaysJoiningWithinTryCreateTxFollowedNot", false);
  }

  @Test
  public void asyncWithNotSupported() {
    assertForOpeartion("asyncWithNotSupported", false);
  }

  @Test
  public void asyncWithJoin() {
    assertForOpeartion("asyncWithJoin", false);
  }

  @Test
  public void asyncWithJoinIfPossible() {
    assertForOpeartion("asyncWithJoinIfPossible", false);
  }

  @Test
  public void tryAndAsyncWithNotSupported() {
    assertForOpeartion("tryAndAsyncWithNotSupported", false);
  }

  @Test
  public void tryAndAsyncJoin() {
    assertForOpeartion("tryAndAsyncJoin", false);
  }

  @Test
  public void tryAndAsyncJoinFollowedByNotSupported() {
    assertForOpeartion("tryAndAsyncJoinFollowedByNotSupported", false);
  }

  @Test
  public void tryAndAsyncJoinFollowedByJoin() {
    assertForOpeartion("tryAndAsyncJoinFollowedByJoin", true);
  }

  @Test
  public void tryAlwaysJoinAndAsyncJoinFollowedByJoin() {
    assertForOpeartion("tryAlwaysJoinAndAsyncJoinFollowedByJoin", false);
  }

  @Test
  public void callingOtherOp() {
    assertForOpeartion("callingOtherOp", true);
  }

  @Test
  public void callingOtherOpNonTx() {
    assertForOpeartion("callingOtherOpNonTx", false);
  }

  @Test
  public void callingOtherOpWithinTryWithTx() {
    assertForOpeartion("callingOtherOpWithinTryWithTx", false);
  }

  @Test
  public void choiceWithOneRouteJoining() {
    assertForOpeartion("choice", true);
  }

  private void assertForOpeartion(String operation, boolean expectedIsTransactional) {
    OperationModel model = getOperationModel(operation);
    assertThat(model.isTransactional(), is(expectedIsTransactional));
    assertThat(model.getAllParameterModels().stream()
        .anyMatch(parameterModel -> parameterModel.getName().equals(TRANSACTIONAL_ACTION_PARAMETER_NAME)), is(false));
  }

  private OperationModel getOperationModel(String name) {
    if (operationModels == null) {
      operationModels = extensionManager.getExtension(muleContext.getConfiguration().getId()).get().getOperationModels();
    }
    return operationModels.stream().filter(opModel -> opModel.getName().equals(name)).findFirst().get();
  }

}

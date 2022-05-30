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
    assertForOperation("withoutTxAction", false);
  }

  @Test
  public void withTxActionNotJoining() {
    assertForOperation("withTxActionNotJoining", false);
  }

  @Test
  public void withTxActionJoining() {
    assertForOperation("withTxActionJoining", true);
  }

  @Test
  public void withTxActionAlwaysJoining() {
    assertForOperation("withTxActionAlwaysJoining", true);
  }

  @Test
  public void withTxActionJoiningWithinTryIndifferent() {
    assertForOperation("withTxActionJoiningWithinTryIndifferent", true);
  }

  @Test
  public void withTxActionNotJoiningWithinTryIndifferent() {
    assertForOperation("withTxActionNotJoiningWithinTryIndifferent", false);
  }

  @Test
  public void withTxActionAlwaysJoiningWithinTryJoining() {
    assertForOperation("withTxActionAlwaysJoiningWithinTryJoining", true);
  }

  @Test
  public void withTxActionNotJoiningWithinTryJoining() {
    assertForOperation("withTxActionNotJoiningWithinTryJoining", false);
  }

  @Test
  public void withTxActionAlwaysJoiningWithinTryCreateTx() {
    assertForOperation("withTxActionAlwaysJoiningWithinTryCreateTx", false);
  }

  @Test
  public void withTxActionAlwaysNotJoiningWithinTryCreateTx() {
    assertForOperation("withTxActionAlwaysNotJoiningWithinTryCreateTx", false);
  }

  @Test
  public void withTxActionAlwaysJoiningWithinTryCreateTxFollowed() {
    assertForOperation("withTxActionAlwaysJoiningWithinTryCreateTxFollowed", true);
  }

  @Test
  public void withTxActionAlwaysJoiningWithinTryCreateTxFollowedNot() {
    assertForOperation("withTxActionAlwaysJoiningWithinTryCreateTxFollowedNot", false);
  }

  @Test
  public void asyncWithNotSupported() {
    assertForOperation("asyncWithNotSupported", false);
  }

  @Test
  public void asyncWithJoin() {
    assertForOperation("asyncWithJoin", false);
  }

  @Test
  public void asyncWithJoinIfPossible() {
    assertForOperation("asyncWithJoinIfPossible", false);
  }

  @Test
  public void tryAndAsyncWithNotSupported() {
    assertForOperation("tryAndAsyncWithNotSupported", false);
  }

  @Test
  public void tryAndAsyncJoin() {
    assertForOperation("tryAndAsyncJoin", false);
  }

  @Test
  public void tryAndAsyncJoinFollowedByNotSupported() {
    assertForOperation("tryAndAsyncJoinFollowedByNotSupported", false);
  }

  @Test
  public void tryAndAsyncJoinFollowedByJoin() {
    assertForOperation("tryAndAsyncJoinFollowedByJoin", true);
  }

  @Test
  public void tryAlwaysJoinAndAsyncJoinFollowedByJoin() {
    assertForOperation("tryAlwaysJoinAndAsyncJoinFollowedByJoin", false);
  }

  @Test
  public void callingOtherOp() {
    assertForOperation("callingOtherOp", true);
  }

  @Test
  public void callingOtherOpNonTx() {
    assertForOperation("callingOtherOpNonTx", false);
  }

  @Test
  public void callingOtherOpWithinTryWithTx() {
    assertForOperation("callingOtherOpWithinTryWithTx", false);
  }

  @Test
  public void choiceWithOneRouteJoining() {
    assertForOperation("choice", true);
  }

  @Test
  public void havingTransactionalActionParameter() {
    OperationModel model = getOperationModel( "operationWithTransactionalActionParameter");
    assertThat(model.isTransactional(), is(false));
    assertThat(model.getAllParameterModels().stream()
            .anyMatch(parameterModel -> parameterModel.getName().equals(TRANSACTIONAL_ACTION_PARAMETER_NAME)), is(true));
  }

  private void assertForOperation(String operation, boolean expectedIsTransactional) {
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

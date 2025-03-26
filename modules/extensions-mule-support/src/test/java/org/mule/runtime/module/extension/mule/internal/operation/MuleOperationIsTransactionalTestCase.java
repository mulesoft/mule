/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.operation;

import static org.mule.runtime.extension.api.ExtensionConstants.TRANSACTIONAL_ACTION_PARAMETER_NAME;
import static org.mule.test.allure.AllureConstants.ReuseFeature.REUSE;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.OPERATIONS;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.extension.ExtensionManager;

import java.util.List;

import jakarta.inject.Inject;

import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(REUSE)
@Story(OPERATIONS)
public class MuleOperationIsTransactionalTestCase extends MuleArtifactFunctionalTestCase {

  @Inject
  private ExtensionManager extensionManager;

  private static List<OperationModel> operationModels;

  @Override
  protected String getConfigFile() {
    return "mule-tx-ops-config.xml";
  }

  @Test
  @Description("Checks that operation without any tx is not transactional")
  public void withoutInnerTransactionalOps() {
    assertForOperation("withoutTxAction", false);
  }

  @Test
  @Description("Checks that operation with operation not joining is not transactional")
  public void withTxActionNotJoining() {
    assertForOperation("withTxActionNotJoining", false);
  }

  @Test
  @Description("Checks that operation with operation that joins tx is transactional")
  public void withTxActionJoining() {
    assertForOperation("withTxActionJoining", true);
  }

  @Test
  @Description("Checks that operation always joining tx is transactional")
  public void withTxActionAlwaysJoining() {
    assertForOperation("withTxActionAlwaysJoining", true);
  }

  @Test
  @Description("Checks that operation joining within try (indifferent) tx is transactional")
  public void withTxActionJoiningWithinTryIndifferent() {
    assertForOperation("withTxActionJoiningWithinTryIndifferent", true);
  }

  @Test
  @Description("Checks that operation not joining within try (indifferent) tx is not transactional")
  public void withTxActionNotJoiningWithinTryIndifferent() {
    assertForOperation("withTxActionNotJoiningWithinTryIndifferent", false);
  }

  @Test
  @Description("Checks that operation always joining within try (join) tx is transactional")
  public void withTxActionAlwaysJoiningWithinTryJoining() {
    assertForOperation("withTxActionAlwaysJoiningWithinTryJoining", true);
  }

  @Test
  @Description("Checks that operation not joining within try (joining) tx is not transactional")
  public void withTxActionNotJoiningWithinTryJoining() {
    assertForOperation("withTxActionNotJoiningWithinTryJoining", false);
  }

  @Test
  @Description("Checks that operation joining within try that creates tx is not transactional")
  public void withTxActionAlwaysJoiningWithinTryCreateTx() {
    assertForOperation("withTxActionAlwaysJoiningWithinTryCreateTx", false);
  }

  @Test
  @Description("Checks that operation not joining within try that creates tx is not transactional")
  public void withTxActionAlwaysNotJoiningWithinTryCreateTx() {
    assertForOperation("withTxActionAlwaysNotJoiningWithinTryCreateTx", false);
  }

  @Test
  @Description("Checks that operation joining within try that creates tx, followed by operation that joins is transactional")
  public void withTxActionAlwaysJoiningWithinTryCreateTxFollowed() {
    assertForOperation("withTxActionAlwaysJoiningWithinTryCreateTxFollowed", true);
  }

  @Test
  @Description("Checks that operation joining within try that creates tx, followed by operation that doesn't join is not transactional")
  public void withTxActionAlwaysJoiningWithinTryCreateTxFollowedNot() {
    assertForOperation("withTxActionAlwaysJoiningWithinTryCreateTxFollowedNot", false);
  }

  @Test
  @Description("Checks that operation not joining within async is not transactional")
  public void asyncWithNotSupported() {
    assertForOperation("asyncWithNotSupported", false);
  }

  @Test
  @Description("Checks that operation always joining within async is not transactional")
  public void asyncWithJoin() {
    assertForOperation("asyncWithJoin", false);
  }

  @Test
  @Description("Checks that operation joining within async is not transactional")
  public void asyncWithJoinIfPossible() {
    assertForOperation("asyncWithJoinIfPossible", false);
  }

  @Test
  @Description("Checks that operation not joining within async within try is not transactional")
  public void tryAndAsyncWithNotSupported() {
    assertForOperation("tryAndAsyncWithNotSupported", false);
  }

  @Test
  @Description("Checks that operation joining within async within try is not transactional")
  public void tryAndAsyncJoin() {
    assertForOperation("tryAndAsyncJoin", false);
  }

  @Test
  @Description("Checks that operation not joining within async within try, followed by not supported is not transactional")
  public void tryAndAsyncJoinFollowedByNotSupported() {
    assertForOperation("tryAndAsyncJoinFollowedByNotSupported", false);
  }

  @Test
  @Description("Checks that operation not joining within async within try, followed by join is transactional")
  public void tryAndAsyncJoinFollowedByJoin() {
    assertForOperation("tryAndAsyncJoinFollowedByJoin", true);
  }

  @Test
  @Description("Checks that operation not joining within async within try (join), followed by not supported is not transactional")
  public void tryAlwaysJoinAndAsyncJoinFollowedByJoin() {
    assertForOperation("tryAlwaysJoinAndAsyncJoinFollowedByJoin", false);
  }

  @Test
  @Description("Checks invoking a transactional operation makes this one transactional")
  public void callingOtherOp() {
    assertForOperation("callingOtherOp", true);
  }

  @Test
  @Description("Checks invoking a non transactional operation keeps this one as non transactional")
  public void callingOtherOpNonTx() {
    assertForOperation("callingOtherOpNonTx", false);
  }

  @Test
  @Description("Checks invoking a transactional operation within try that creates tx makes this one non transactional")
  public void callingOtherOpWithinTryWithTx() {
    assertForOperation("callingOtherOpWithinTryWithTx", false);
  }

  @Test
  @Description("Checks that choice with one route joining, makes the operation transactional")
  public void choiceWithOneRouteJoining() {
    assertForOperation("choice", true);
  }

  @Test
  @Description("Checks that an operation can have 'transactionalAction' parameter")
  public void havingTransactionalActionParameter() {
    OperationModel model = getOperationModel("operationWithTransactionalActionParameter");
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

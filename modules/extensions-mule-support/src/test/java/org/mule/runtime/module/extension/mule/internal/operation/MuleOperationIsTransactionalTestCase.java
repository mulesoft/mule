/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.operation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;

import org.junit.Test;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.extension.ExtensionManager;

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
    assertThat(getOperationModel("withoutTxAction").isTransactional(), is(false));
  }

  @Test
  public void withTxActionNotJoining() {
    assertThat(getOperationModel("withTxActionNotJoining").isTransactional(), is(false));
  }

  @Test
  public void withTxActionJoining() {
    assertThat(getOperationModel("withTxActionJoining").isTransactional(), is(true));
  }

  @Test
  public void withTxActionAlwaysJoining() {
    assertThat(getOperationModel("withTxActionAlwaysJoining").isTransactional(), is(true));
  }

  @Test
  public void withTxActionJoiningWithinTryIndifferent() {
    assertThat(getOperationModel("withTxActionJoiningWithinTryIndifferent").isTransactional(), is(true));
  }

  @Test
  public void withTxActionNotJoiningWithinTryIndifferent() {
    assertThat(getOperationModel("withTxActionNotJoiningWithinTryIndifferent").isTransactional(), is(false));
  }

  @Test
  public void withTxActionAlwaysJoiningWithinTryJoining() {
    assertThat(getOperationModel("withTxActionAlwaysJoiningWithinTryJoining").isTransactional(), is(true));
  }

  @Test
  public void withTxActionNotJoiningWithinTryJoining() {
    assertThat(getOperationModel("withTxActionNotJoiningWithinTryJoining").isTransactional(), is(false));
  }

  @Test
  public void withTxActionAlwaysJoiningWithinTryCreateTx() {
    assertThat(getOperationModel("withTxActionAlwaysJoiningWithinTryCreateTx").isTransactional(), is(false));
  }

  @Test
  public void withTxActionAlwaysNotJoiningWithinTryCreateTx() {
    assertThat(getOperationModel("withTxActionAlwaysNotJoiningWithinTryCreateTx").isTransactional(), is(false));
  }

  @Test
  public void withTxActionAlwaysJoiningWithinTryCreateTxFollowed() {
    assertThat(getOperationModel("withTxActionAlwaysJoiningWithinTryCreateTxFollowed").isTransactional(), is(true));
  }

  @Test
  public void withTxActionAlwaysJoiningWithinTryCreateTxFollowedNot() {
    assertThat(getOperationModel("withTxActionAlwaysJoiningWithinTryCreateTxFollowedNot").isTransactional(), is(false));
  }

  @Test
  public void asyncWithNotSupported() {
    assertThat(getOperationModel("asyncWithNotSupported").isTransactional(), is(false));
  }

  @Test
  public void asyncWithJoin() {
    assertThat(getOperationModel("asyncWithJoin").isTransactional(), is(false));
  }

  @Test
  public void asyncWithJoinIfPossible() {
    assertThat(getOperationModel("asyncWithJoinIfPossible").isTransactional(), is(false));
  }

  @Test
  public void tryAndAsyncWithNotSupported() {
    assertThat(getOperationModel("tryAndAsyncWithNotSupported").isTransactional(), is(false));
  }

  @Test
  public void tryAndAsyncJoin() {
    assertThat(getOperationModel("tryAndAsyncJoin").isTransactional(), is(false));
  }

  @Test
  public void tryAndAsyncJoinFollowedByNotSupported() {
    assertThat(getOperationModel("tryAndAsyncJoinFollowedByNotSupported").isTransactional(), is(false));
  }

  @Test
  public void tryAndAsyncJoinFollowedByJoin() {
    assertThat(getOperationModel("tryAndAsyncJoinFollowedByJoin").isTransactional(), is(true));
  }

  @Test
  public void tryAlwaysJoinAndAsyncJoinFollowedByJoin() {
    assertThat(getOperationModel("tryAlwaysJoinAndAsyncJoinFollowedByJoin").isTransactional(), is(false));
  }

  @Test
  public void choiceWithOneRouteJoining() {
    assertThat(getOperationModel("choice").isTransactional(), is(true));
  }

  private OperationModel getOperationModel(String name) {
    if (operationModels == null) {
      operationModels = extensionManager.getExtension(muleContext.getConfiguration().getId()).get().getOperationModels();
    }
    return operationModels.stream().filter(opModel -> opModel.getName().equals(name)).findFirst().get();
  }

}

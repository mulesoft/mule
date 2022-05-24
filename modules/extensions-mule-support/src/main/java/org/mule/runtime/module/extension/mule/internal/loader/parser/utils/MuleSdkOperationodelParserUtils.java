/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser.utils;

import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.SCOPE;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.ASYNC_IDENTIFIER;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.TRY_IDENTIFIER;
import static org.mule.runtime.core.api.transaction.MuleTransactionConfig.ACTION_ALWAYS_BEGIN_STRING;
import static org.mule.runtime.extension.api.ExtensionConstants.TRANSACTIONAL_ACTION_PARAMETER_NAME;

import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.extension.api.tx.OperationTransactionalAction;

import java.util.List;

public class MuleSdkOperationodelParserUtils {

  private static boolean isTry(ComponentAst componentAst) {
    return componentAst.getIdentifier().equals(TRY_IDENTIFIER);
  }

  public static boolean isSkippedScopeForTx(ComponentAst componentAst) {
    if (!componentAst.getComponentType().equals(SCOPE)) {
      return false;
    }
    if (componentAst.getIdentifier().equals(ASYNC_IDENTIFIER)) {
      return true;
    }
    if (isTry(componentAst)) {
      ComponentParameterAst transactionalAction =
          componentAst.getParameter(DEFAULT_GROUP_NAME, TRANSACTIONAL_ACTION_PARAMETER_NAME);
      return transactionalAction != null && transactionalAction.getValue().getValue().get().equals(ACTION_ALWAYS_BEGIN_STRING);
    }
    return false;
  }

  public static boolean isIgnoredComponentForTx(ComponentAst componentAst) {
    ComponentParameterAst transactionalAction =
        componentAst.getParameter(DEFAULT_GROUP_NAME, TRANSACTIONAL_ACTION_PARAMETER_NAME);
    return transactionalAction != null && !isTry(componentAst) && OperationTransactionalAction
        .valueOf(transactionalAction.getValue().getValue().get().toString()).equals(OperationTransactionalAction.NOT_SUPPORTED);
  }

  public static boolean areAllCharacteristicsWithDefinitiveValue(List<Characteristic<?>> characteristics) {
    return characteristics.stream().allMatch(Characteristic::hasDefinitiveValue);
  }

}

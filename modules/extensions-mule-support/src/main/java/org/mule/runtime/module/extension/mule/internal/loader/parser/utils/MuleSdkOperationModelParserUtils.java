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

import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.extension.api.tx.OperationTransactionalAction;
import org.mule.runtime.module.extension.mule.internal.loader.parser.utils.Characteristic.ComponentAstWithHierarchy;

/**
 * Utils class to check on {@link ComponentAst} particularities for parsing different features (in
 * {@link org.mule.runtime.module.extension.mule.internal.loader.parser.MuleSdkOperationModelParserSdk})
 *
 * @since 4.5
 */
public class MuleSdkOperationModelParserUtils {

  private static boolean isTry(ComponentAst componentAst) {
    return componentAst.getIdentifier().equals(TRY_IDENTIFIER);
  }

  /**
   * @param componentAst
   * @return if a component (and children) should be skipped for considered for isTransactional calculation
   */
  public static boolean isSkippedScopeForTx(ComponentAstWithHierarchy componentAst) {
    ComponentAst ast = componentAst.getComponentAst();
    if (!ast.getComponentType().equals(SCOPE)) {
      return false;
    }
    if (ast.getIdentifier().equals(ASYNC_IDENTIFIER)) {
      return true;
    }
    if (isTry(ast)) {
      ComponentParameterAst transactionalAction = ast.getParameter(DEFAULT_GROUP_NAME, TRANSACTIONAL_ACTION_PARAMETER_NAME);
      return transactionalAction != null && transactionalAction.getValue().getValue().get().equals(ACTION_ALWAYS_BEGIN_STRING);
    }
    return false;
  }

  /**
   * @param componentAst
   * @return if this particular component should be ignored for considered for isTransactional calculation
   */
  public static boolean isIgnoredComponentForTx(ComponentAstWithHierarchy componentAst) {
    ComponentAst ast = componentAst.getComponentAst();
    if (!ast.getModel(ParameterizedModel.class).isPresent()) {
      return false;
    }
    ComponentParameterAst transactionalAction = ast.getParameter(DEFAULT_GROUP_NAME, TRANSACTIONAL_ACTION_PARAMETER_NAME);
    return transactionalAction != null && !isTry(ast) && OperationTransactionalAction
        .valueOf(transactionalAction.getValue().getValue().get().toString()).equals(OperationTransactionalAction.NOT_SUPPORTED);
  }

  private MuleSdkOperationModelParserUtils() {
    // Private constructor to prevent instantiation.
  }
}

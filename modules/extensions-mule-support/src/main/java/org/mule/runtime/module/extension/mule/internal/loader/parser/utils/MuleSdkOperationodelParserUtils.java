/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser.utils;

import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;

import java.util.List;

import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.SCOPE;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.ASYNC_IDENTIFIER;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.TRY_IDENTIFIER;

public class MuleSdkOperationodelParserUtils {

  public static boolean isSkippedScopeForTx(ComponentAst componentAst) {
    if (!componentAst.getComponentType().equals(SCOPE)) {
      return false;
    }
    if (componentAst.getIdentifier().equals(ASYNC_IDENTIFIER)) {
      return true;
    }
    if (componentAst.getIdentifier().equals(TRY_IDENTIFIER)) {
      ComponentParameterAst transactionalAction = componentAst.getParameter("General", "transactionalAction");
      return transactionalAction != null && transactionalAction.getRawValue().equals("ALWAYS_BEGIN");
    }
    return false;
  }

  public static boolean areAllCharacteristicsWithDefinitiveValue(List<Characteristic<?>> characteristics) {
    return characteristics.stream().allMatch(Characteristic::hasDefinitiveValue);
  }

  public static void setToDefaultIfNeeded(List<Characteristic<?>> characteristics) {
    for (Characteristic<?> characteristic : characteristics) {
      if (!characteristic.hasValue()) {
        characteristic.setWithDefault();
      }
    }
  }

}

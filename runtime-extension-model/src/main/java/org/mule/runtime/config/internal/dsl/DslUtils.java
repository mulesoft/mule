/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl;

import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.config.internal.dsl.processor.xml.OperationDslNamespaceInfoProvider.OPERATION_DSL_NAMESPACE;
import static org.mule.runtime.internal.dsl.DslConstants.BODY_NAME;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import static org.mule.runtime.internal.dsl.DslConstants.TRY_SCOPE_NAME;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.ast.api.ComponentAst;

/**
 * Utility class used to make some simple checks on the DSL.
 */
public final class DslUtils {

  public static final ComponentIdentifier OPERATION_BODY_IDENTIFIER =
      builder().namespace(OPERATION_DSL_NAMESPACE).name(BODY_NAME).build();
  public static final ComponentIdentifier TRY_SCOPE_IDENTIFIER = builder().namespace(CORE_PREFIX).name(TRY_SCOPE_NAME).build();

  private DslUtils() {
    // Empty constructor to prevent it from being accidentally instantiated.
  }

  /**
   * Checks whether a given {@link ComponentAst} corresponds to a try scope.
   *
   * @param componentAst the component AST.
   * @return {@code true} if the given {@link ComponentAst} is a try scope, or {@code false} otherwise.
   */
  public static boolean isTryScope(ComponentAst componentAst) {
    return TRY_SCOPE_IDENTIFIER.equals(componentAst.getIdentifier());
  }
}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.el.mvel;

import static java.lang.Thread.currentThread;

/**
 * Resolves variables accessing to Mule internal API.
 *
 * @since 4.0
 */
public class MuleInternalAliasVariableResolver extends MuleAliasVariableResolver {

  /**
   * Creates a new varaible resolver
   *
   * @param name variable name to ve resolved.
   * @param expression expression used to replace the variable name
   * @param context expression execution context.
   */
  MuleInternalAliasVariableResolver(String name, String expression, MVELExpressionLanguageContext context) {
    super(name, expression, context);
  }

  @Override
  public Object getValue() {
    ClassLoader contextClassLoader = currentThread().getContextClassLoader();

    try {
      currentThread().setContextClassLoader(this.getClass().getClassLoader());
      return super.getValue();
    } finally {
      currentThread().setContextClassLoader(contextClassLoader);
    }
  }
}

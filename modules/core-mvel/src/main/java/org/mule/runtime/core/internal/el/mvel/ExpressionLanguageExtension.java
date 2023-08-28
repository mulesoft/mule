/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.el.mvel;

/**
 * Interface used to implement extensions to be used with a Mule Expression Language implementation. Imports, variables, aliases
 * and functions can be added to the Expression Language context as required via the methods available in the
 * {@link ExpressionLanguageContext} instance provided. Note: The context provided, is a static context with no message context
 * available. In order to work with per-evaluation context including the message (including it's payload and properties) and
 * flow/session variables then you should implement {@link ExpressionLanguageFunction}'s and make these available in the context.
 * 
 * @since 3.3
 */
public interface ExpressionLanguageExtension {

  void configureContext(ExpressionLanguageContext context);

}

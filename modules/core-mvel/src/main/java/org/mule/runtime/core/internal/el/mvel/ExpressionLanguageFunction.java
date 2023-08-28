/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.el.mvel;

/**
 * @since 3.3
 */
public interface ExpressionLanguageFunction {

  Object call(Object[] params, ExpressionLanguageContext context);

}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.connector.basic;

import org.mule.extension.api.annotation.Expression;
import org.mule.extension.api.annotation.Parameter;
import org.mule.extension.api.introspection.ExpressionSupport;

public class Account
{

    @Parameter
    @Expression(ExpressionSupport.REQUIRED)
    private Owner requiredInnerPojoWithExpressionRequired;

}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.connector.basic;

import org.mule.extension.api.annotation.Expression;
import org.mule.extension.api.annotation.Extension;
import org.mule.extension.api.annotation.Operations;
import org.mule.extension.api.annotation.Parameter;
import org.mule.extension.api.introspection.ExpressionSupport;

@Extension(name = "Basic", description = "Basic Test connector")
@Operations(VoidOperations.class)
public class GlobalInnerPojoConnector
{

    @Parameter
    @Expression(ExpressionSupport.REQUIRED)
    private Account requiredPojoExpressionRequired;

    /**
     * This should generate a Global element for the inner Owner, but no child element inside the account POJO
     */
    @Parameter
    private Account requiredPojoDefault;
}

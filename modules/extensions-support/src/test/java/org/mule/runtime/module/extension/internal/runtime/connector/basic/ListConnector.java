/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connector.basic;

import org.mule.extension.api.annotation.Expression;
import org.mule.extension.api.annotation.Extension;
import org.mule.extension.api.annotation.Operations;
import org.mule.extension.api.annotation.Parameter;
import org.mule.extension.api.annotation.param.Optional;
import org.mule.extension.api.introspection.parameter.ExpressionSupport;

import java.util.List;

@Extension(name = "List", description = "List Test connector")
@Operations(VoidOperations.class)
public class ListConnector
{

    @Parameter
    private List<Object> requiredListDefaults;

    @Parameter
    @Expression(ExpressionSupport.NOT_SUPPORTED)
    private List<Object> requiredListNoExpressions;

    @Parameter
    @Expression(ExpressionSupport.REQUIRED)
    private List<Object> requiredListExpressionRequireds;

    @Parameter
    @Expression(ExpressionSupport.SUPPORTED)
    private List<Object> requiredListExpressionSupporteds;

    @Parameter
    @Optional
    private List<Object> optionalListDefaults;

    @Parameter
    @Expression(ExpressionSupport.NOT_SUPPORTED)
    @Optional
    private List<Object> optionalListNoExpressions;

    @Parameter
    @Expression(ExpressionSupport.REQUIRED)
    @Optional
    private List<Object> optionalListExpressionRequireds;

    @Parameter
    @Expression(ExpressionSupport.SUPPORTED)
    @Optional
    private List<Object> optionalListExpressionSupporteds;

}

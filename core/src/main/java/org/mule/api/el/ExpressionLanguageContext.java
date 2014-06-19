/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.el;

import java.lang.reflect.Method;

/**
 * @since 3.3
 */
public interface ExpressionLanguageContext
{

    void importClass(Class<?> clazz);

    void importClass(String name, Class<?> clazz);

    void importStaticMethod(String name, Method method);

    <T> void addVariable(String name, T value);

    <T> void addVariable(String name, T value, VariableAssignmentCallback<T> assignmentCallback);

    <T> void addFinalVariable(String name, T value);

    void addAlias(String alias, String expression);

    void declareFunction(String name, ExpressionLanguageFunction function);

    <T> T getVariable(String name);

    <T> T getVariable(String name, Class<T> type);

    boolean contains(String name);

    /**
     *  DEPRECATED: Use addVariable() or addFinalVariable() instead depending if variable should be 
     *  assignable or not.
     */
    @Deprecated
    <T> void addPrivateVariable(String name, T value);

}

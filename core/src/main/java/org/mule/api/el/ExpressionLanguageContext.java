/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

}

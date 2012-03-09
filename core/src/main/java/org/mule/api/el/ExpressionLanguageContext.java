/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.el;

import java.lang.reflect.Method;

public interface ExpressionLanguageContext
{

    void importClass(Class<?> clazz);

    void importClass(String name, Class<?> clazz);

    void importStaticMethod(String name, Method method);

    void addVariable(String name, Object value);

    void addFinalVariable(String name, Object value);

    void addAlias(String alias, String expression);

    void declareFunction(String name, ExpressionLanguageFunction function);

    <T> T getVariable(String name);

    <T> T getVariable(String name, Class<T> type);

    boolean contains(String name);

}

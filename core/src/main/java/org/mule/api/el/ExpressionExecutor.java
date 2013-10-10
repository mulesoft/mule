/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.el;

/**
 * Wraps an expression language engine. Implementations should not wrap expression language engine exceptions,
 * but rather the {@link ExpressionLanguage} implementation should handle them.
 * 
 * @since 3.3
 */
public interface ExpressionExecutor<T extends ExpressionLanguageContext>
{

    /**
     * Execute an expression using using the provided context.
     * 
     * @param expression
     * @param context
     * @return
     * @throws native expression language
     */
    public Object execute(String expression, T context);

    /**
     * Validate the expression
     * 
     * @param expression
     */
    public void validate(String expression);

}

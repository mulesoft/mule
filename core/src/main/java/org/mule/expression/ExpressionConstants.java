/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.expression;

/**
 * Constnts for Headers and Attachment processing shared by a number of the Expression evaluators
 */
public class ExpressionConstants
{

    private ExpressionConstants()
    {
        // do not instantiate
    }

    public static final String DELIM = ",";
    public static final String ALL_ARGUMENT = "*";
    public static final String OPTIONAL_ARGUMENT = "?";
}

/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.generic.ParentDefinitionParser;
import org.mule.config.spring.parsers.processors.ExtendTarget;

public class IgnoreObjectMethodsDefinitionParser extends ParentDefinitionParser
{

    public static final String IGNORED_METHOD = "ignoredMethod";

    public IgnoreObjectMethodsDefinitionParser()
    {
        registerPostProcessor(new ExtendTarget(IGNORED_METHOD, "toString"));
        registerPostProcessor(new ExtendTarget(IGNORED_METHOD, "hashCode"));
        registerPostProcessor(new ExtendTarget(IGNORED_METHOD, "wait"));
        registerPostProcessor(new ExtendTarget(IGNORED_METHOD, "notify"));
        registerPostProcessor(new ExtendTarget(IGNORED_METHOD, "notifyAll"));
        registerPostProcessor(new ExtendTarget(IGNORED_METHOD, "getClass"));
    }

}

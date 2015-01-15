/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.springconfig.parsers.specific;

import org.mule.module.springconfig.parsers.generic.ParentDefinitionParser;
import org.mule.module.springconfig.parsers.processors.ExtendTarget;

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

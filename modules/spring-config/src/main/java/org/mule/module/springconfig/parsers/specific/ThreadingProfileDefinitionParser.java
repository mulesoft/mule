/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.springconfig.parsers.specific;

import org.mule.api.config.ThreadingProfile;
import org.mule.config.ChainedThreadingProfile;
import org.mule.module.springconfig.parsers.generic.ChildDefinitionParser;
import org.mule.module.springconfig.parsers.processors.ConstructorReference;

/**
 * This parser is responsible for processing the <code><threading-profile><code> configuration elements.
 */
public class ThreadingProfileDefinitionParser extends ChildDefinitionParser
{

    public ThreadingProfileDefinitionParser(String propertyName, String defaults)
    {
        super(propertyName, ChainedThreadingProfile.class);
        addMapping("poolExhaustedAction", ThreadingProfile.POOL_EXHAUSTED_ACTIONS);
        registerPostProcessor(new ConstructorReference(defaults));
    }

}

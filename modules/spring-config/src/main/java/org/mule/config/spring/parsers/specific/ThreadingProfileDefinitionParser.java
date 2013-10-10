/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.api.config.ThreadingProfile;
import org.mule.config.ChainedThreadingProfile;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.processors.ConstructorReference;

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

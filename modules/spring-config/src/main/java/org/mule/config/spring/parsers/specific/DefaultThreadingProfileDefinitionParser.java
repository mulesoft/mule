/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.api.config.ThreadingProfile;
import org.mule.config.ChainedThreadingProfile;
import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.config.spring.parsers.processors.IdAttribute;
import org.mule.config.spring.parsers.processors.NameAttribute;

/**
 * This parser is responsible for processing the <code><threading-profile><code> configuration elements.
 */
public class DefaultThreadingProfileDefinitionParser extends OrphanDefinitionParser
{

    public DefaultThreadingProfileDefinitionParser(String propertyName)
    {
        super(ChainedThreadingProfile.class, true);
        addMapping("poolExhaustedAction", ThreadingProfile.POOL_EXHAUSTED_ACTIONS);
        registerPostProcessor(new IdAttribute(propertyName));
        registerPostProcessor(new NameAttribute(propertyName));
    }

}

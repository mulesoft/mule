/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.api.config.ThreadingProfile;
import org.mule.config.ChainedThreadingProfile;
import org.mule.config.DirectThreadingProfile;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.processors.ConstructorReference;

/**
 * This parser is responsible for processing the configuration element for {@link org.mule.config.DirectThreadingProfile}.
 */
public class DirectThreadingProfileDefinitionParser extends ChildDefinitionParser
{

    public DirectThreadingProfileDefinitionParser(String propertyName)
    {
        super(propertyName, DirectThreadingProfile.class);
        addMapping("poolExhaustedAction", ThreadingProfile.POOL_EXHAUSTED_ACTIONS);
    }

}

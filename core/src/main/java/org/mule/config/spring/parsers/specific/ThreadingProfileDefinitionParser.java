/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.ThreadingProfile;

/**
 * This parser is responsible for processing the <code><threading-profile><code> configuration elements.
 */
public class ThreadingProfileDefinitionParser extends ConfigurationChildDefinitionParser
{

    public ThreadingProfileDefinitionParser(String propertyName)
    {
        super(propertyName, ThreadingProfile.class);
        addAlias("poolExhaustedAction", "poolExhaustedActionString");
        addIgnored(ATTRIBUTE_NAME);
    }

}

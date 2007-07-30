/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.PoolingProfile;

/**
 * This parser is responsible for processing the <code><pooling-profile><code> configuration elements.
 */
public class PoolingProfileDefinitionParser extends ConfigurationChildDefinitionParser
{

    public PoolingProfileDefinitionParser()
    {
        super("poolingProfile", PoolingProfile.class);
        addAlias("initialisationPolicy", "initialisationPolicyString");
        addAlias("exhaustedAction", "exhaustedActionString");
    }

}

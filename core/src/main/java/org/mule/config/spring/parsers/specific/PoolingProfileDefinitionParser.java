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

import org.mule.config.PoolingProfile;

/**
 * This parser is responsible for processing the <code><pooling-profile><code> configuration elements.
 */
public class PoolingProfileDefinitionParser extends ConfigurationChildDefinitionParser
{

    public PoolingProfileDefinitionParser()
    {
        super("poolingProfile", PoolingProfile.class);
        addMapping("initialisationPolicy", PoolingProfile.POOL_INITIALISATION_POLICIES);
        addMapping("exhaustedAction", PoolingProfile.POOL_EXHAUSTED_ACTIONS);
    }

}

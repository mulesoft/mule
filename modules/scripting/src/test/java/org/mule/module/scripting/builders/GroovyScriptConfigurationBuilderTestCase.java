/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.scripting.builders;

import org.mule.api.MuleException;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.module.scripting.builders.ScriptConfigurationBuilder;
import org.mule.tck.AbstractScriptConfigBuilderTestCase;

public class GroovyScriptConfigurationBuilderTestCase extends AbstractScriptConfigBuilderTestCase
{

    public String getConfigResources()
    {
        return "mule-config.groovy";
    }

    // @Override
    public ConfigurationBuilder getBuilder()
    {
        try
        {
            return new ScriptConfigurationBuilder("groovy", getConfigResources());
        }
        catch (MuleException e)
        {
            fail(e.getMessage());
            return null;
        }
    }

}

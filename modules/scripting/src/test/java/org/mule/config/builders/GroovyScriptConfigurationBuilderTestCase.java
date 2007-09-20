/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.builders;

import org.mule.config.ConfigurationBuilder;
import org.mule.tck.AbstractScriptConfigBuilderTestCase;
import org.mule.umo.UMOException;

public class GroovyScriptConfigurationBuilderTestCase extends AbstractScriptConfigBuilderTestCase
{

    public String getConfigResources()
    {
        // TODO MULE-2205 Update script to use registry instead of QuickConfigurationBuilder
        return "mule-config.groovy";
    }

    // @Override
    public ConfigurationBuilder getBuilder()
    {
        try
        {
            ScriptConfigurationBuilder scb = new ScriptConfigurationBuilder("groovy");
            scb.setStartContext(false);
            return scb;
        }
        catch (UMOException e)
        {
            fail(e.getMessage());
            return null;
        }
    }

}

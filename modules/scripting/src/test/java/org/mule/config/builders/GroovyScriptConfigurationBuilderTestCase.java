/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.builders;

import org.mule.config.ConfigurationBuilder;
import org.mule.config.builders.ScriptConfigurationBuilder;
import org.mule.tck.AbstractScriptConfigBuilderTestCase;

/**
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class GroovyScriptConfigurationBuilderTestCase extends AbstractScriptConfigBuilderTestCase
{
    public String getConfigResources() {
        return "mule-config.groovy";
    }

    public ConfigurationBuilder getBuilder() {
       ScriptConfigurationBuilder builder = new ScriptConfigurationBuilder("groovy");
        return builder;
    }
}

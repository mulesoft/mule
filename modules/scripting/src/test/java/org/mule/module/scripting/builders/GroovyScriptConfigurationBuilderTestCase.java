/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.scripting.builders;

import static org.junit.Assert.fail;
import org.mule.api.MuleException;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.tck.AbstractScriptConfigBuilderTestCase;

import java.util.List;

public class GroovyScriptConfigurationBuilderTestCase extends AbstractScriptConfigBuilderTestCase
{
    @Override
    public String getConfigFile()
    {
        return "mule-config.groovy";
    }

    @Override
    public ConfigurationBuilder getBuilder()
    {
        try
        {
            return new ScriptConfigurationBuilder("groovy", getConfigFile());
        }
        catch (MuleException e)
        {
            fail(e.getMessage());
            return null;
        }
    }

    @Override
    protected void addBuilders(List<ConfigurationBuilder> builders)
    {
        try
        {
            builders.add(0, new SpringXmlConfigurationBuilder(new String[] {}));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}

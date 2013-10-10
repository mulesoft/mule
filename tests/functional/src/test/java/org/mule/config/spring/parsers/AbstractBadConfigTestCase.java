/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.api.config.ConfigurationBuilder;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A stripped-down version of FunctionalTestCase that allows us to test the parsing of a bad configuration. 
 */
public abstract class AbstractBadConfigTestCase extends AbstractServiceAndFlowTestCase
{
    private String configResources;
    
    public AbstractBadConfigTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, "");
        this.configResources = configResources;
    }

    protected final transient Log logger = LogFactory.getLog(getClass());

    public void assertErrorContains(String phrase) throws Exception
    {
        try 
        {
            parseConfig();
            fail("expected error");
        } 
        catch (Exception e) 
        {            
            logger.debug("Caught " + e);
            assertTrue("Missing phrase '" + phrase + "' in '" + e.toString() + "'",
                    e.toString().indexOf(phrase) > -1);
        }
    }
    
    protected void parseConfig() throws Exception
    {
        new DefaultMuleContextFactory().createMuleContext(getConfigurationBuilder());
    }

    protected ConfigurationBuilder getConfigurationBuilder() throws Exception
    {
        return new SpringXmlConfigurationBuilder(configResources);
    }
}

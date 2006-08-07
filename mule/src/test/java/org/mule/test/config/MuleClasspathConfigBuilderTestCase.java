/**
 * 
 */
package org.mule.test.config;

import org.mule.config.ConfigurationBuilder;
import org.mule.config.ConfigurationException;
import org.mule.config.builders.MuleClasspathConfigurationBuilder;

/**
 * Test Case for MuleClasspathCofigurationBuilder. It borrows everything
 * from the corresponding MuleXml config builder and just substitute the
 * builder itself since the config files are in the tests classpath.
 * 
 * @author Massimo Lusetti <mlusetti@gmail.com>
 */
public class MuleClasspathConfigBuilderTestCase extends
        MuleXmlConfigBuilderTestCase
{
    /**
     * Get the builder
     */
    public ConfigurationBuilder getBuilder()
    {
        try {
            return new MuleClasspathConfigurationBuilder();
        } catch (ConfigurationException e) {
            fail(e.getMessage());
            return null;
        }
    }
}

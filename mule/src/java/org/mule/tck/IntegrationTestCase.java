/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.tck;

import org.mule.config.ConfigurationBuilder;
import org.mule.config.MuleProperties;
import org.mule.config.builders.MuleXmlConfigurationBuilder;

/**
 * Is a base tast case for tests that initialise Mule using a configuration file
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public abstract class IntegrationTestCase extends AbstractMuleTestCase
{
    protected final void doSetUp() throws Exception {
        System.setProperty(MuleProperties.DISABLE_SERVER_CONNECTIONS, "false");
        ConfigurationBuilder builder = getBuilder();
        builder.configure(getConfigResources());
        doIntegrationSetUp();
    }

    protected final void doTearDown() throws Exception {
        doIntegrationTearDown();
    }

    protected ConfigurationBuilder getBuilder() throws Exception {
        return new MuleXmlConfigurationBuilder();
    }

    protected void doIntegrationSetUp() throws Exception {

    }

    protected void doIntegrationTearDown() throws Exception {

    }


    protected abstract String getConfigResources();

}

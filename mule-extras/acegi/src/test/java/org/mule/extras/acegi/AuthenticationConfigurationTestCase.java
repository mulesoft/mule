/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.extras.acegi;

import org.mule.MuleManager;
import org.mule.config.builders.MuleXmlConfigurationBuilder;

/**
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class AuthenticationConfigurationTestCase extends HttpBasicEndpointFilterTestCase
{
     public void setUp() throws Exception
    {
        if(MuleManager.isInstanciated()) MuleManager.getInstance().dispose();
        MuleXmlConfigurationBuilder builder = new MuleXmlConfigurationBuilder();
        builder.configure("test-acegi-http-config.xml");
    }

    protected void tearDown() throws Exception
    {
        MuleManager.getInstance().dispose();
    }
}

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
package org.mule.test.integration.spring;

import junit.framework.TestCase;

import org.mule.extras.client.MuleClient;
import org.mule.extras.client.RemoteDispatcher;
import org.mule.extras.spring.config.SpringConfigurationBuilder;
import org.mule.umo.UMOMessage;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class MuleAdminTestCase extends TestCase {
	
	public void test() throws Exception
	{
		SpringConfigurationBuilder builder = new SpringConfigurationBuilder();
        builder.configure("org/mule/test/integration/spring/mule-admin-spring.xml");
        
        MuleClient mc = new MuleClient();
        RemoteDispatcher rd = mc.getRemoteDispatcher("tcp://localhost:60504");
        UMOMessage result = rd.sendToRemoteComponent("appleComponent", "string", null);
        assertNotNull(result);
	}

}

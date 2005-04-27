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
 *
 */
package org.mule.extras.quartz;

import junit.framework.TestCase;

import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.umo.manager.UMOManager;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class JobBeanTestCase extends TestCase {

	public void test() throws Exception {
		MuleXmlConfigurationBuilder builder = new MuleXmlConfigurationBuilder();
		UMOManager manager = builder.configure("org/mule/extras/quartz/quartz-mule.xml");

		long timeout = System.currentTimeMillis();
		while (System.currentTimeMillis() - timeout < 10000) {
			if (DummyComponent.countCalled.get() > 2) {
				return;
			}
		}
		fail();
	}

}

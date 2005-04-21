package org.mule.extras.quartz;

import junit.framework.TestCase;

import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.umo.manager.UMOManager;

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

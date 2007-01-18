package org.mule.modules.osgi;

import org.mule.MuleServer;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class MuleServerActivator implements BundleActivator {
	
    private MuleServer mule;

	public void start(BundleContext bc) throws Exception {
    	mule = new MuleServer();
        mule.start(false);
	}

	public void stop(BundleContext bc) throws Exception {
        mule.shutdown();
	}
}

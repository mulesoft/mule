/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.osgi;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.springframework.osgi.test.ConfigurableBundleCreatorTests;

/**
 * The test classes in this project will be turned into a virtual bundle 
 * which is installed and the tests are then run inside the OSGi runtime.
 */
public class MuleBundleTest extends ConfigurableBundleCreatorTests {

	protected String getPlatformName()
    {
        return KNOPFLERFISH_PLATFORM;
    }

    /**
	 * The manifest to use for the "virtual bundle" created
	 * out of the test classes and resources in this project
	 */
	protected String getManifestLocation() 
    { 
		return "classpath:org/mule/osgi/test/MANIFEST.MF";
	}
	
	/**
	 * The location of the packaged OSGi bundles to be installed
	 * for this test. Values are Spring resource paths. The bundles
	 * we want to use are part of the same multi-project maven
	 * build as this project is. Hence we use the localMavenArtifact
	 * helper method to find the bundles produced by the package
	 * phase of the maven build (these tests will run after the
	 * packaging phase, in the integration-test phase). 
	 * 
	 * JUnit, commons-logging, spring-core and the spring OSGi
	 * test bundle are automatically included so do not need
	 * to be specified here.
	 */
	protected String[] getBundleLocations() 
    {
		return new String[] {
            // Spring OSGi
			localMavenArtifact("org.springframework.osgi", "aopalliance.osgi","1.0-SNAPSHOT"),
			localMavenArtifact("org.springframework.osgi", "spring-context", "2.1-SNAPSHOT"),
			localMavenArtifact("org.springframework.osgi", "spring-beans","2.1-SNAPSHOT"),
			localMavenArtifact("org.springframework.osgi", "spring-osgi-core","1.0-SNAPSHOT"),
			localMavenArtifact("org.springframework.osgi", "spring-osgi-extender","1.0-SNAPSHOT"),
            localMavenArtifact("org.springframework.osgi", "spring-aop","2.1-SNAPSHOT"),
            // PAX Logging
            localMavenArtifact("org.ops4j.pax.logging", "api","0.9.4"),
            localMavenArtifact("org.ops4j.pax.logging", "service","0.9.4"),
            localMavenArtifact("org.ops4j.pax.logging", "log4j","0.9.4"),
            localMavenArtifact("org.ops4j.pax.logging", "slf4j","0.9.4"),
            localMavenArtifact("org.ops4j.pax.logging", "jcl","0.9.4"),
            // Mule
            localMavenArtifact("org.mule", "mule-core","2.0-REGISTRY"),
            localMavenArtifact("org.mule.modules", "mule-module-osgi","2.0-REGISTRY")
            //localMavenArtifact("org.mule.examples", "mule-example-hello-osgi","2.0-REGISTRY")
		};
	}
	
	/**
	 * The superclass provides us access to the root bundle
	 * context via the 'getBundleContext' operation
	 */
	public void testOSGiStartedOk() {
		BundleContext bundleContext = getBundleContext();
		assertNotNull(bundleContext);
	}
	
    public void testMuleBundlesExported() {
        BundleContext context = getBundleContext();
        assertTrue("Bundle should be loaded in the OSGi framework.", isBundleLoaded(context, "org.mule.core"));
        
        assertFalse("Bundle should not be loaded in the OSGi framework.", isBundleLoaded(context, "org.mule.bogus"));
    }
        
//    public void testMuleServiceExported() {
//        waitOnContextCreation("org.mule.core");
//        BundleContext context = getBundleContext();
//        ServiceReference ref = context.getServiceReference(MuleSoaManager.class.getName());
//        assertNotNull("Service Reference is null", ref);
//        try {
//            UMOManager manager = (UMOManager) context.getService(ref);
//            assertNotNull("Cannot find the service", manager);
//            // TODO What do we test for?
//            //assertEquals("something",managementContext.getSomething());
//        } finally {
//            context.ungetService(ref);
//        }
//    }

    // This method is in OsgiUtils, but gives linker errors when called from another bundle, so we repeat it here for now.
    public static boolean isBundleLoaded(BundleContext context, String symbolicName) {
        Bundle[] bundles = context.getBundles();
        for (int i=0; i<bundles.length; ++i) {
            if (bundles[i].getSymbolicName().equals(symbolicName)) {
                return true;
            }
        }
        return false;
      }      
}

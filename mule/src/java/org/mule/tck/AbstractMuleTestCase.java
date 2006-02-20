/*
 * $Header:
 * /cvsroot/mule/mule/src/test/org/mule/test/mule/AbstractMuleTestCase.java,v
 * 1.7 2003/11/24 09:58:47 rossmason Exp $ $Revision$ $Date: 2003/11/24
 * 09:58:47 $
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved. http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 *  
 */

package org.mule.tck;

import junit.framework.TestCase;
import org.mule.MuleManager;
import org.mule.config.MuleConfiguration;
import org.mule.impl.MuleDescriptor;
import org.mule.tck.testmodels.mule.TestConnector;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.manager.UMOManager;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.StringMessageHelper;
import org.mule.util.Utility;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * <code>AbstractMuleTestCase</code> is a base class for Mule testcases. This
 * implementation provides services to test code for creating mock and test
 * objects.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public abstract class AbstractMuleTestCase extends TestCase {

    //This should be set to a string message describing any prerequisites not met
    protected String prereqs = null;
    private boolean offline = System.getProperty("org.mule.offline", "false").equalsIgnoreCase("true");
    private boolean testLogging = System.getProperty("org.mule.test.logging", "false").equalsIgnoreCase("true");

    private static Map testCounters;

    public AbstractMuleTestCase() {
        super();
        if(testCounters==null) {
            testCounters = new HashMap();
        }
        addTest();
    }

    protected void addTest() {
        TestInfo info = (TestInfo)testCounters.get(getClass().getName());
        if(info==null) {
            info = new TestInfo(getClass().getName());
            testCounters.put(getClass().getName(), info);
        }
        info.incTestCount();
    }

    protected void setDisposeManagerPerSuite(boolean val) {
        getTestInfo().setDisposeManagerPerSuite(val);
    }

    protected TestInfo getTestInfo() {
        TestInfo info = (TestInfo)testCounters.get(getClass().getName());
        if(info==null) {
            info = new TestInfo(getClass().getName());
            testCounters.put(getClass().getName(), info);
        }
        return info;
    }

    private void clearAllCounters() {
        if(testCounters!=null) {
            testCounters.clear();
            testCounters=null;
        }
        log("Cleared all counters");
    }

    private void clearCounter() {
        if(testCounters!=null) {
            testCounters.remove(getClass().getName());
        }
        log("Cleared counter: " + getClass().getName());
    }

    private void log(String s) {
        if(testLogging) System.err.println(s);
    }

    public String getName() {
        return super.getName().substring(4).replaceAll("([A-Z])", " $1").toLowerCase();
    }

    /**
     * Use this method to do any validation such as check for an installation of a required server
     * If the current environment does not have the preReqs of the test return false and the test will
     * be skipped.
     *
     * @return
     */
    protected String checkPreReqs() {
        return null;
    }

    public boolean isOffline(String method) {
        if (offline) {
            System.out.println(StringMessageHelper.getBoilerPlate("Working offline cannot run test: " + method, '=', 80));
        }
        return offline;
    }

    public boolean isPrereqsMet(String method) {
        prereqs = checkPreReqs();
        if (prereqs != null) {
            System.out.println(StringMessageHelper.getBoilerPlate("WARNING\nPrerequisites for test: " + method + " were not met. skipping test: " + prereqs, '=', 80));
        }
        ;
        return prereqs == null;
    }

    protected final void setUp() throws Exception
    {
        System.out.println(StringMessageHelper.getBoilerPlate("Testing: " + toString(), '=', 80));
        MuleManager.getConfiguration().getDefaultThreadingProfile().setDoThreading(false);
        MuleManager.getConfiguration().setServerUrl(Utility.EMPTY_STRING);

        try {
            if (getTestInfo().getRunCount() == 0) {
                if (getTestInfo().isDisposeManagerPerSuite()) {
                    //We dispose here jut in case
                    disposeManager();
                }
                log("Pre suiteSetup for test: " + getTestInfo());
                suitePreSetUp();
            }
            if (!getTestInfo().isDisposeManagerPerSuite()) {
                //We dispose here jut in case
                disposeManager();
            }
            if (!isPrereqsMet(getClass().getName() + ".setUp()")) return;
            doSetUp();
            if (getTestInfo().getRunCount() == 0) {
                log("Post suiteSetup for test: " + getTestInfo());
                suitePostSetUp();
            }
        } catch (Exception e) {
            getTestInfo().incRunCount();
            throw e;
        }
    }

    protected void suitePreSetUp() throws Exception {

    }
    protected void suitePostSetUp() throws Exception {

    }

    protected void suitePreTearDown() throws Exception {

    }
    protected void suitePostTearDown() throws Exception {

    }

    protected final void tearDown() throws Exception {
        try {
             if (getTestInfo().getRunCount() == getTestInfo().getTestCount()) {
                    log("Pre suiteTearDown for test: " + getTestInfo());
                    suitePreTearDown();
             }
            doTearDown();
            if (!getTestInfo().isDisposeManagerPerSuite()) {
                disposeManager();
            }
        } finally {
            getTestInfo().incRunCount();
            if (getTestInfo().getRunCount() == getTestInfo().getTestCount()) {
                try {
                    log("Post suiteTearDown for test: " + getTestInfo());
                    suitePostTearDown();
                } finally {
                    clearCounter();
                    disposeManager();
                }
            }
        }
    }

    protected void disposeManager() {
        log("disposing manager. disposeManagerPerSuite=" + getTestInfo().isDisposeManagerPerSuite());
        if (MuleManager.isInstanciated()) {
            MuleManager.getInstance().dispose();
        }
        Utility.deleteTree(new File(MuleManager.getConfiguration().getWorkingDirectory()));
        Utility.deleteTree(new File("./ActiveMQ"));
        MuleManager.setConfiguration(new MuleConfiguration());
    }

    protected void doSetUp() throws Exception {

    }

    protected void doTearDown() throws Exception {

    }

    public static UMOManager getManager(boolean disableAdminAgent) throws Exception {
        return MuleTestUtils.getManager(disableAdminAgent);
    }

    public static UMOEndpoint getTestEndpoint(String name, String type) throws Exception {
        return MuleTestUtils.getTestEndpoint(name, type);
    }

    public static UMOEvent getTestEvent(Object data) throws Exception {
        return MuleTestUtils.getTestEvent(data);
    }

    public static UMOTransformer getTestTransformer() {
        return MuleTestUtils.getTestTransformer();
    }

    public static UMOEvent getTestEvent(Object data, MuleDescriptor descriptor) throws Exception {
        return MuleTestUtils.getTestEvent(data, descriptor);
    }

    public static UMOEvent getTestEvent(Object data, UMOEndpoint endpoint) throws Exception {
        return MuleTestUtils.getTestEvent(data, endpoint);
    }

    public static UMOEvent getTestEvent(Object data, MuleDescriptor descriptor, UMOEndpoint endpoint)
            throws UMOException {
        return MuleTestUtils.getTestEvent(data, descriptor, endpoint);
    }

    public static UMOSession getTestSession(UMOComponent component) {
        return MuleTestUtils.getTestSession(component);
    }

    public static TestConnector getTestConnector() {
        return MuleTestUtils.getTestConnector();
    }

    public static UMOComponent getTestComponent(MuleDescriptor descriptor) {
        return MuleTestUtils.getTestComponent(descriptor);
    }

    public static MuleDescriptor getTestDescriptor(String name, String implementation) throws Exception {
        return MuleTestUtils.getTestDescriptor(name, implementation);
    }

    public static UMOManager getTestManager() throws UMOException {
        return MuleTestUtils.getTestManager();
    }

    protected void finalize() throws Throwable {
        clearAllCounters();
    }

    protected class TestInfo {
        /**
         * Whether to dispose the manager after every method or once all tests for the class have run
         */
        private boolean disposeManagerPerSuite = false;
        private int testCount = 0;
        private int runCount = 0;
        private String name;

        public TestInfo(String name) {
            this.name = name;
        }


        public void clearCounts() {
            testCount = 0;
            runCount = 0;
            log("Cleared counts for: " + name);
        }


        public void incTestCount() {
            testCount++;
            log("Added test: " + name + " " + testCount);
        }


        public void incRunCount() {
            runCount++;
            log("Finished Run: " + toString());
        }

        public int getTestCount() {
            return testCount;
        }

        public int getRunCount() {
            return runCount;
        }

        public String getName() {
            return name;
        }

        public boolean isDisposeManagerPerSuite() {
            return disposeManagerPerSuite;
        }

        public void setDisposeManagerPerSuite(boolean disposeManagerPerSuite) {
            this.disposeManagerPerSuite = disposeManagerPerSuite;
        }

        public String toString() {
            StringBuffer buf = new StringBuffer();
            return buf.append(name).append(", (").append(runCount).append(" / ")
                    .append(testCount).append(") tests run, disposePerSuite=")
                    .append(disposeManagerPerSuite).toString();
        }
    }
}

/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck;

import org.mule.RegistryContext;
import org.mule.impl.MuleDescriptor;
import org.mule.tck.testmodels.mule.TestConnector;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOException;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.model.UMOModel;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.FileUtils;
import org.mule.util.StringMessageUtils;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import junit.framework.TestCase;

/**
 * <code>AbstractMuleTestCase</code> is a base class for Mule testcases. This
 * implementation provides services to test code for creating mock and test objects.
 */
public abstract class AbstractMuleTestCase extends TestCase
{
    protected final Log logger = LogFactory.getLog(getClass());

    // This should be set to a string message describing any prerequisites not met
    protected String prereqs = null;
    private boolean offline = System.getProperty("org.mule.offline", "false").equalsIgnoreCase("true");
    private boolean testLogging = System.getProperty("org.mule.test.logging", "false").equalsIgnoreCase(
        "true");

    private static Map testCounters;

    protected static UMOManagementContext managementContext;

    public AbstractMuleTestCase()
    {
        super();
        if (testCounters == null)
        {
            testCounters = new HashMap();
        }
        addTest();
    }

    protected void addTest()
    {
        TestInfo info = (TestInfo)testCounters.get(getClass().getName());
        if (info == null)
        {
            info = new TestInfo(getClass().getName());
            testCounters.put(getClass().getName(), info);
        }
        info.incTestCount();
    }

    protected void setDisposeManagerPerSuite(boolean val)
    {
        getTestInfo().setDisposeManagerPerSuite(val);
    }

    protected TestInfo getTestInfo()
    {
        TestInfo info = (TestInfo)testCounters.get(getClass().getName());
        if (info == null)
        {
            info = new TestInfo(getClass().getName());
            testCounters.put(getClass().getName(), info);
        }
        return info;
    }

    private void clearAllCounters()
    {
        if (testCounters != null)
        {
            testCounters.clear();
        }
        log("Cleared all counters");
    }

    private void clearCounter()
    {
        if (testCounters != null)
        {
            testCounters.remove(getClass().getName());
        }
        log("Cleared counter: " + getClass().getName());
    }

    private void log(String s)
    {
        if (testLogging)
        {
            System.err.println(s);
        }
    }

    public String getName()
    {
        return super.getName().substring(4).replaceAll("([A-Z])", " $1").toLowerCase() + " ";
    }

    /**
     * Use this method to do any validation such as check for an installation of a
     * required server If the current environment does not have the preReqs of the
     * test return false and the test will be skipped.
     * 
     */
    protected String checkPreReqs()
    {
        return null;
    }

    public boolean isOffline(String method)
    {
        if (offline)
        {
            System.out.println(StringMessageUtils.getBoilerPlate(
                "Working offline cannot run test: " + method, '=', 80));
        }
        return offline;
    }

    public boolean isPrereqsMet(String method)
    {
        prereqs = checkPreReqs();
        if (prereqs != null)
        {
            System.out.println(StringMessageUtils.getBoilerPlate(
                "WARNING\nPrerequisites for test: " + method + " were not met. skipping test: " + prereqs,
                '=', 80));
        }
        return prereqs == null;
    }

    protected final void setUp() throws Exception
    {
        System.out.println(StringMessageUtils.getBoilerPlate("Testing: " + toString(), '=', 80));
        

        try
        {
            if (getTestInfo().getRunCount() == 0)
            {
                if (getTestInfo().isDisposeManagerPerSuite())
                {
                    // We dispose here jut in case
                    disposeManager();
                }
                log("Pre suiteSetup for test: " + getTestInfo());
                suitePreSetUp();
            }
            if (!getTestInfo().isDisposeManagerPerSuite())
            {
                // We dispose here jut in case
                disposeManager();
            }
            if (!isPrereqsMet(getClass().getName() + ".setUp()"))
            {
                return;
            }
            managementContext = createManagementContext();

            doSetUp();
            if (getTestInfo().getRunCount() == 0)
            {
                log("Post suiteSetup for test: " + getTestInfo());
                suitePostSetUp();
            }
        }
        catch (Exception e)
        {
            getTestInfo().incRunCount();
            throw e;
        }
    }

    protected UMOManagementContext createManagementContext() throws Exception
    {
        //This will create the local registry too
        ApplicationContext ctx = new ClassPathXmlApplicationContext("default-mule-config.xml");

        UMOManagementContext managementContext = (UMOManagementContext)ctx.getBean("_muleManagementContextFactoryBean");
        //Add a default model for compoennts to run in
        managementContext.getRegistry().registerModel(getDefaultModel(managementContext));
        return managementContext;
    }
    protected void suitePreSetUp() throws Exception
    {
        // nothing to do
    }

    protected void suitePostSetUp() throws Exception
    {
        // nothing to do
    }

    protected void suitePreTearDown() throws Exception
    {
        // nothing to do
    }

    protected void suitePostTearDown() throws Exception
    {
        // nothing to do
    }

    protected final void tearDown() throws Exception
    {
        try
        {
            if (getTestInfo().getRunCount() == getTestInfo().getTestCount())
            {
                log("Pre suiteTearDown for test: " + getTestInfo());
                suitePreTearDown();
            }
            doTearDown();
            if (!getTestInfo().isDisposeManagerPerSuite())
            {
                disposeManager();
            }
        }
        finally
        {
            getTestInfo().incRunCount();
            if (getTestInfo().getRunCount() == getTestInfo().getTestCount())
            {
                try
                {
                    log("Post suiteTearDown for test: " + getTestInfo());
                    suitePostTearDown();
                }
                finally
                {
                    clearCounter();
                    disposeManager();
                }
            }
        }
    }

    protected void disposeManager()
    {
        try
        {
            log("disposing manager. disposeManagerPerSuite=" + getTestInfo().isDisposeManagerPerSuite());
            if (managementContext!=null)
            {
                FileUtils.deleteTree(FileUtils.newFile(RegistryContext.getConfiguration().getWorkingDirectory()));
                managementContext.dispose();
            }
            FileUtils.deleteTree(FileUtils.newFile("./ActiveMQ"));
        }
        finally
        {
            managementContext = null;
        }
    }

    protected void doSetUp() throws Exception
    {
        // template method
    }

    protected void doTearDown() throws Exception
    {
        // template method
    }

    public static UMOModel getDefaultModel(UMOManagementContext context) throws UMOException
    {
        return MuleTestUtils.getDefaultModel(context);
    }

    public static UMOEndpoint getTestEndpoint(String name, String type) throws Exception
    {
        return MuleTestUtils.getTestEndpoint(name, type, managementContext);
    }

    public static UMOEvent getTestEvent(Object data) throws Exception
    {
        return MuleTestUtils.getTestEvent(data, managementContext);
    }

    public static UMOEventContext getTestEventContext(Object data) throws Exception
    {
        return MuleTestUtils.getTestEventContext(data, managementContext);
    }

    public static UMOTransformer getTestTransformer()
    {
        return MuleTestUtils.getTestTransformer();
    }

    public static UMOEvent getTestEvent(Object data, MuleDescriptor descriptor) throws Exception
    {
        return MuleTestUtils.getTestEvent(data, descriptor, managementContext);
    }

    public static UMOEvent getTestEvent(Object data, UMOImmutableEndpoint endpoint) throws Exception
    {
        return MuleTestUtils.getTestEvent(data, endpoint, managementContext);
    }

    public static UMOEvent getTestEvent(Object data, MuleDescriptor descriptor, UMOImmutableEndpoint endpoint)
        throws UMOException
    {
        return MuleTestUtils.getTestEvent(data, descriptor, endpoint);
    }

    public static UMOSession getTestSession(UMOComponent component)
    {
        return MuleTestUtils.getTestSession(component);
    }

    public static TestConnector getTestConnector()
    {
        return MuleTestUtils.getTestConnector();
    }

    public static UMOComponent getTestComponent(MuleDescriptor descriptor)
    {
        return MuleTestUtils.getTestComponent(descriptor);
    }

    public static MuleDescriptor getTestDescriptor(String name, String implementation) throws Exception
    {
        return MuleTestUtils.getTestDescriptor(name, implementation, managementContext);
    }

    protected void finalize() throws Throwable
    {
        try
        {
            clearAllCounters();
        }
        finally
        {
            super.finalize();
        }
    }

    protected class TestInfo
    {
        /**
         * Whether to dispose the manager after every method or once all tests for
         * the class have run
         */
        private boolean disposeManagerPerSuite = false;
        private int testCount = 0;
        private int runCount = 0;
        private String name;

        public TestInfo(String name)
        {
            this.name = name;
        }

        public void clearCounts()
        {
            testCount = 0;
            runCount = 0;
            log("Cleared counts for: " + name);
        }

        public void incTestCount()
        {
            testCount++;
            log("Added test: " + name + " " + testCount);
        }

        public void incRunCount()
        {
            runCount++;
            log("Finished Run: " + toString());
        }

        public int getTestCount()
        {
            return testCount;
        }

        public int getRunCount()
        {
            return runCount;
        }

        public String getName()
        {
            return name;
        }

        public boolean isDisposeManagerPerSuite()
        {
            return disposeManagerPerSuite;
        }

        public void setDisposeManagerPerSuite(boolean disposeManagerPerSuite)
        {
            this.disposeManagerPerSuite = disposeManagerPerSuite;
        }

        public String toString()
        {
            StringBuffer buf = new StringBuffer();
            return buf.append(name).append(", (").append(runCount).append(" / ").append(testCount).append(
                ") tests run, disposePerSuite=").append(disposeManagerPerSuite).toString();
        }
    }
}

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
package org.mule.test.integration.providers.jms.activemq;

import java.io.File;
import java.util.HashMap;
import java.util.Properties;

import javax.jms.Connection;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.ExecuteWatchdog;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.Watchdog;
import org.mule.MuleManager;
import org.mule.config.PoolingProfile;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.MuleModel;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.impl.internal.events.ConnectionEvent;
import org.mule.impl.internal.events.ConnectionEventListener;
import org.mule.providers.SimpleRetryConnectionStrategy;
import org.mule.providers.file.filters.FilenameWildcardFilter;
import org.mule.providers.jms.JmsConnector;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.test.integration.providers.jms.AbstractJmsFunctionalTestCase;
import org.mule.test.integration.providers.jms.tools.JmsTestUtils;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.manager.UMOServerEvent;
import org.mule.umo.provider.UMOConnector;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

/**
 * 
 * This test needs the path to an activemq distribution.
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 *
 */
public class JmsConnectionTestCase extends AbstractJmsFunctionalTestCase implements ConnectionEventListener {

	private static final String ACTIVEMQ_HOME = "c:\\java\\activemq-3.0";
	
	private JmsConnector connector;
	private KillableWatchdog activemq;
	private LinkedQueue events = new LinkedQueue();
	
    protected void setUp() throws Exception
    {
        if (MuleManager.isInstanciated())
            MuleManager.getInstance().dispose();
        // By default the JmsTestUtils use the openjms config, though you can
        // pass
        // in other configs using the property below

        // Make sure we are running synchronously
        MuleManager.getConfiguration().setSynchronous(true);
        MuleManager.getConfiguration()
                   .getPoolingProfile()
                   .setInitialisationPolicy(PoolingProfile.POOL_INITIALISE_ONE_COMPONENT);

        MuleManager.getInstance().setModel(new MuleModel());
        callbackCalled = false;
        MuleManager.getInstance().registerConnector(createConnector());
        currentMsg = null;
        eventCount = 0;
    }
    
    protected void tearDown() throws Exception
    {
    	killActiveMq();
    }
    
    public UMOConnector createConnector() throws Exception
    {
        connector = new JmsConnector();
        connector.setSpecification(JmsConnector.JMS_SPECIFICATION_11);
        Properties props = JmsTestUtils.getJmsProperties(JmsTestUtils.ACTIVE_MQ_JMS_PROPERTIES);

        connector.setConnectionFactoryJndiName("JmsQueueConnectionFactory");
        connector.setProviderProperties(props);
        connector.setName(CONNECTOR_NAME);
        connector.getDispatcherThreadingProfile().setDoThreading(false);

        SimpleRetryConnectionStrategy strategy = new SimpleRetryConnectionStrategy();
        strategy.setRetryCount(10);
        strategy.setFrequency(5000);
        strategy.setDoThreading(true);
        connector.setConnectionStrategy(strategy);
        
        return connector;
    }

    public Connection getConnection() throws Exception
    {
        // default to ActiveMq for Jms 1.1 support
        Properties p = JmsTestUtils.getJmsProperties(JmsTestUtils.ACTIVE_MQ_JMS_PROPERTIES);
        return JmsTestUtils.getQueueConnection(p);
    }

	public void testReconnection() throws Exception {

        MuleDescriptor d = getTestDescriptor("anOrange", Orange.class.getName());

        UMOComponent component = MuleManager.getInstance().getModel().registerComponent(d);
        UMOEndpoint endpoint = new MuleEndpoint("test",
                                                new MuleEndpointURI("jms://my.queue"),
                                                connector,
                                                null,
                                                UMOEndpoint.ENDPOINT_TYPE_SENDER,
                                                0,
                                                new HashMap());
        MuleManager.getInstance().start();
        MuleManager.getInstance().registerListener(this);
        connector.registerListener(component, endpoint);

        // Start time
        long t0;
        // Check that connection fails
        t0 = System.currentTimeMillis();
        while (true) {
        	ConnectionEvent event = (ConnectionEvent) events.take();
        	if (event.getAction() == ConnectionEvent.CONNECTION_FAILED) {
        		break;
        	}
        	if (System.currentTimeMillis() - t0 < 10000) {
        		fail("No connection attempt");
        	}
        }
        
        // Launch activemq
        launchActiveMq();
        // Check that connection succeed
        t0 = System.currentTimeMillis();
        while (true) {
        	ConnectionEvent event = (ConnectionEvent) events.take();
        	if (event.getAction() == ConnectionEvent.CONNECTION_CONNECTED) {
        		break;
        	}
        	if (System.currentTimeMillis() - t0 < 10000) {
        		fail("Connection should have succeeded");
        	}
        }
        // Kill activemq
        killActiveMq();
        // Check that the connection is lost
        t0 = System.currentTimeMillis();
        while (true) {
        	ConnectionEvent event = (ConnectionEvent) events.take();
        	if (event.getAction() == ConnectionEvent.CONNECTION_DISCONNECTED) {
        		break;
        	}
        	if (System.currentTimeMillis() - t0 < 10000) {
        		fail("Connection should have been lost");
        	}
        }
        // Restart activemq
        launchActiveMq();
        // Check that connection succeed
        t0 = System.currentTimeMillis();
        while (true) {
        	ConnectionEvent event = (ConnectionEvent) events.take();
        	if (event.getAction() == ConnectionEvent.CONNECTION_CONNECTED) {
        		break;
        	}
        	if (System.currentTimeMillis() - t0 < 10000) {
        		fail("Connection should have succeeded");
        	}
        }
        killActiveMq();

	}
	
	protected void launchActiveMq() {
		Project project = new Project();
		DefaultLogger consoleLogger = new DefaultLogger();
		consoleLogger.setErrorPrintStream(System.err);
		consoleLogger.setOutputPrintStream(System.out);
		consoleLogger.setMessageOutputLevel(Project.MSG_INFO);
		project.addBuildListener(consoleLogger);
		Path path = new Path(project);
		File[] jars = new File(ACTIVEMQ_HOME + "\\lib").listFiles(new FilenameWildcardFilter("*.jar"));
		path.add(new Path(project, new File(ACTIVEMQ_HOME, "\\conf").getAbsolutePath()));
		for (int i = 0; i < jars.length; i++) {
			path.add(new Path(project, jars[i].getAbsolutePath()));
		}
		jars = new File(ACTIVEMQ_HOME + "\\lib\\optional").listFiles(new FilenameWildcardFilter("*.jar"));
		for (int i = 0; i < jars.length; i++) {
			path.add(new Path(project, jars[i].getAbsolutePath()));
		}
		final JavaTask java = new JavaTask();
		java.setProject(project);
		java.setClasspath(path);
		java.setClassname("org.activemq.spring.Main");
		java.setFork(true);
		java.setDir(new File(ACTIVEMQ_HOME));
		java.addSysproperty(createVar("activemq.home", new File(ACTIVEMQ_HOME).getAbsolutePath()));
		java.addSysproperty(createVar("derby.system.home", new File(ACTIVEMQ_HOME, "\\var").getAbsolutePath()));
		java.createWatchdog();
		new Thread() {
			public void run() {
				java.execute();
			}
		}.start();
		activemq = java.watchDog;
	}
	
	private static class JavaTask extends Java {
		public KillableWatchdog watchDog;
		private Long timeout = new Long(Long.MAX_VALUE);
		public void setTimeout(Long value) {
			this.timeout = value;
			super.setTimeout(value);
		}
	    protected ExecuteWatchdog createWatchdog() throws BuildException {
	    	if (watchDog == null) {
	    		watchDog = new KillableWatchdog(timeout != null ? timeout.longValue() : 0);
	    	}
	    	return watchDog;
	    }

	}
	
	private static class KillableWatchdog extends ExecuteWatchdog {
		private Process process;
		public KillableWatchdog(long timeout) {
			super(timeout);
		}
		public void timeoutOccured(Watchdog w) {
		}
		public synchronized void start(Process process) {
			this.process = process;
			super.start(process);
		}
		public void kill() {
			super.timeoutOccured(null);
		}
	}
	
	private static Environment.Variable createVar(String name, String value) {
		Environment.Variable var = new Environment.Variable();
		var.setKey(name);
		var.setValue(value);
		return var;
	}
	
	protected void killActiveMq() {
		if (activemq != null ) {
			activemq.kill();
		}
	}

	public void onEvent(UMOServerEvent event) {
		try {
			events.put(event);
		} catch (InterruptedException e) {
		}
	}

}

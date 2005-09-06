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
package org.mule.test.integration;

import org.activemq.ActiveMQConnection;
import org.activemq.ActiveMQConnectionFactory;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.ExecuteWatchdog;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.Watchdog;
import org.mule.providers.file.filters.FilenameWildcardFilter;

import javax.jms.JMSException;
import java.io.File;

/**
 * Will start external test servers needed for the integration tests
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */

public class ServerTools
{
    private static final String ACTIVEMQ_HOME = "org.activemq.home";

    private static KillableWatchdog activemq;
    private static ActiveMQConnectionFactory embeddedFactory = null;

    public static void launchActiveMq()  {
        launchActiveMq(ActiveMQConnection.DEFAULT_BROKER_URL);
    }

    public static ActiveMQConnectionFactory launchEmbeddedActiveMq() throws JMSException {
        embeddedFactory = new ActiveMQConnectionFactory();
        embeddedFactory.setUseEmbeddedBroker(true);
        embeddedFactory.start();
        return embeddedFactory;
    }

    public static void killEmbeddedActiveMq()  {
        if(embeddedFactory != null) {
            try {
                embeddedFactory.stop();
            } catch (JMSException e) {
                
            }
            embeddedFactory = null;
        }
    }
    public static void launchActiveMq(String brokerUrl) {
        String activeMqHome = System.getProperty(ACTIVEMQ_HOME);
        if(activeMqHome == null) {
            throw new NullPointerException("You must set the " + ACTIVEMQ_HOME + " system property to the root path of an ActiveMq distribution (v3.0 and greater) before running these tests");
        }
        Project project = new Project();
        DefaultLogger consoleLogger = new DefaultLogger();
        consoleLogger.setErrorPrintStream(System.err);
        consoleLogger.setOutputPrintStream(System.out);
        consoleLogger.setMessageOutputLevel(Project.MSG_INFO);
        project.addBuildListener(consoleLogger);
        Path path = new Path(project);
        File[] jars = new File(activeMqHome + "\\lib").listFiles(new FilenameWildcardFilter("*.jar"));
        path.add(new Path(project, new File(activeMqHome, "\\conf").getAbsolutePath()));
        for (int i = 0; i < jars.length; i++) {
            path.add(new Path(project, jars[i].getAbsolutePath()));
        }
        jars = new File(activeMqHome + "\\lib\\optional").listFiles(new FilenameWildcardFilter("*.jar"));
        for (int i = 0; i < jars.length; i++) {
            path.add(new Path(project, jars[i].getAbsolutePath()));
        }
        final JavaTask java = new JavaTask();
        java.setProject(project);
        java.setClasspath(path);
        java.setClassname("org.activemq.broker.impl.Main");
        java.setArgs(brokerUrl);
        java.setFork(true);
        java.setDir(new File(activeMqHome));
        java.addSysproperty(createVar("activemq.home", new File(activeMqHome).getAbsolutePath()));
        java.addSysproperty(createVar("derby.system.home", new File(activeMqHome, "\\var").getAbsolutePath()));
        java.createWatchdog();
        new Thread() {
            public void run() {
                java.execute();
            }
        }.start();
        activemq = java.watchDog;
    }

    public static void killActiveMq() {
        if (activemq != null ) {
            activemq.kill();
        }
    }

    static class JavaTask extends Java {
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

    static class KillableWatchdog extends ExecuteWatchdog {
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

    static Environment.Variable createVar(String name, String value) {
        Environment.Variable var = new Environment.Variable();
        var.setKey(name);
        var.setValue(value);
        return var;
    }
}

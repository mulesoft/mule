/*
 * Copyright 2005 SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 * 
 * ------------------------------------------------------------------------------------------------------
 * $Header$
 * $Revision$
 * $Date$
 */
package org.mule.jbi.ant;

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public abstract class AbstractJbiTask extends Task {

    public static final String PROPERTY_DEFAULT_USERNAME = "jbi.default.username";
    public static final String PROPERTY_DEFAULT_PASSWORD = "jbi.default.password";
    public static final String PROPERTY_DEFAULT_HOST = "jbi.default.host";
    public static final String PROPERTY_DEFAULT_PORT = "jbi.default.port";
    public static final String PROPERTY_DEFAULT_URL = "jbi.default.url";
    
    public static final String DEFAULT_URL = "service:jmx:rmi:///jndi/rmi://{0}:{1}/server";
    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = 8000;
    
    private String host;
    private int port;
    private String username;
    private String password;
    private boolean failOnError = true;
    
	public boolean isFailOnError() {
		return this.failOnError;
	}
	public void setFailOnError(boolean failOnError) {
		this.failOnError = failOnError;
	}
	public String getHost() {
		String host = this.host;
		if (host == null) {
			host =getProject().getProperty(PROPERTY_DEFAULT_HOST);
		}
		if (host == null) {
			host = DEFAULT_HOST;
		}
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public String getPassword() {
		if (this.password == null) {
			return getProject().getProperty(PROPERTY_DEFAULT_PASSWORD);
		}
		return this.password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public int getPort() {
		int port = this.port; 
		if (port == 0) {
			port = Integer.parseInt(getProject().getProperty(PROPERTY_DEFAULT_PORT));
		}
		if (port == 0) {
			port = DEFAULT_PORT;
		}
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public String getUsername() {
		return this.username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	
	protected String getUrlTemplate() {
		String url = getProject().getProperty(PROPERTY_DEFAULT_URL);
		if (url == null) {
			url = DEFAULT_URL;
		}
		return url;
	}
	
	protected JMXServiceURL getJmxUrl() throws MalformedURLException {
		String url = MessageFormat.format(getUrlTemplate(), new Object[] { getHost(), Integer.toString(getPort()) });
		return new JMXServiceURL(url);
	}
	
	protected MBeanServerConnection getConnection() throws IOException {
        Map env = new HashMap();
        env.put("jmx.remote.credentials", new String[] { getUsername(), getPassword() });
		JMXConnector con = JMXConnectorFactory.connect(getJmxUrl(), env);
		return con.getMBeanServerConnection();
	}
	
	protected ObjectName getServiceName(String name) throws MalformedObjectNameException {
		return new ObjectName("mule-jbi:type=service,name=" + name);
	}
	
	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Task#execute()
	 */
	public final void execute() throws BuildException {
		try {
			doExecute(getConnection());
		} catch (Exception e) {
			if (this.failOnError) {
				throw new BuildException(e);
			} else {
				log("Error: " + e.getMessage());
			}
		}
	}
	
	protected abstract void doExecute(MBeanServerConnection server);
    
}

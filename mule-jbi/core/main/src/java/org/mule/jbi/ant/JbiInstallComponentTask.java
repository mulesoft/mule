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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.apache.tools.ant.BuildException;

public class JbiInstallComponentTask extends AbstractJbiTask {

	private String file;
	private String paramsFile;
	private List nestedParams;
	
	public String getFile() {
		return this.file;
	}
	public void setFile(String file) {
		this.file = file;
	}
	public String getParams() {
		return this.paramsFile;
	}
	public void setParams(String params) {
		this.paramsFile = params;
	}
	public Param addParam() {
		Param p = new Param();
		if (this.nestedParams == null) {
			this.nestedParams = new ArrayList();
		}
		this.nestedParams.add(p);
		return p;
	}
	
	public static class Param {
		private String name;
		private String value;
		public String getName() {
			return this.name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getValue() {
			return this.value;
		}
		public void setValue(String value) {
			this.value = value;
		}
	}
	
	protected Map getParameters() throws IOException {
		Map params = new HashMap();
		if (this.nestedParams != null) {
			for (Iterator iter = this.nestedParams.iterator(); iter.hasNext();) {
				Param p = (Param) iter.next();
				params.put(p.getName(), p.getValue());
			}
		}
		if (paramsFile != null) {
			Properties props = new Properties();
			props.load(new FileInputStream(paramsFile));
			params.putAll(props);
		}
		return params;
	}
	
	protected AttributeList getAttributes() throws IOException {
		Map parameters = getParameters();
		AttributeList attributes = new AttributeList();
		for (Iterator iter = parameters.keySet().iterator(); iter.hasNext();) {
			String name = (String) iter.next();
			attributes.add(new Attribute(name, parameters.get(name)));
		}
		return attributes;
	}

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Task#execute()
	 */
	public void doExecute(MBeanServerConnection server) throws BuildException {
		try {
			ObjectName service = getServiceName("install");
			ObjectName installer = (ObjectName) server.invoke(service, "loadNewInstaller", new Object[] { file }, new String[] { "java.lang.String" });
			AttributeList attributes = getAttributes();
			if (attributes.size() > 0) {
				ObjectName config = (ObjectName) server.invoke(installer, "getInstallerConfigurationMBean", null, null);
				if (config == null) {
					if (isFailOnError()) {
						throw new BuildException("Configuration parameters are defined, but no bean found");
					} else {
						log("Configuration parameters are defined, but no bean found");
					}
				} else {
					server.setAttributes(config, attributes);
				}
			}
			server.invoke(installer, "install", null, null);
			
		} catch (Exception e) {
			throw new BuildException(e);
		}
	}
	
}

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
package org.mule.jbi.engines.agila;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;

import javax.jbi.JBIException;

import net.sf.hibernate.cfg.Configuration;
import net.sf.hibernate.tool.hbm2ddl.SchemaExport;

import org.dom4j.Document;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.mule.jbi.components.SimpleBootstrap;

public class AgilaBootstrap extends SimpleBootstrap {

	/* (non-Javadoc)
	 * @see org.mule.jbi.components.SimpleBootstrap#doInstall()
	 */
	protected void doInstall() throws Exception {
		// Default installation
		super.doInstall();
		// Get install / workspace dir
		String installRoot = this.installContext.getContext().getInstallRoot();
		String workspaceRoot = this.installContext.getContext().getWorkspaceRoot();
		// Initialize hsqldb url
		File hsqldbDir = new File(workspaceRoot, "hsqldb");
		hsqldbDir.mkdirs();
		File hibernateConfigFile = new File(installRoot, "/resources/hibernate.cfg.xml");
		if (!hibernateConfigFile.isFile()) {
			hibernateConfigFile = new File(Thread.currentThread().getContextClassLoader().getResource("hibernate.cfg.xml").toURI());
		}
		if (!hibernateConfigFile.isFile()) {
			throw new FileNotFoundException("Could not find hibernate.cfg.xml file");
		}
		Document hibCfg = new SAXReader().read(hibernateConfigFile);
		Node node = hibCfg.selectSingleNode("//property[@name = 'connection.url'");
		String url = "jdbc:hsqldb:" + hsqldbDir.getCanonicalFile().toURI() + "agila";
		node.setText(url);
		FileWriter w = new FileWriter(hibernateConfigFile);
		w.write(hibCfg.asXML());
		w.close();
		// Create hsqldb schema
		new SchemaExport(new Configuration().configure()).create(false, true);
	}

}

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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.PortType;
import javax.wsdl.Service;
import javax.wsdl.factory.WSDLFactory;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerFactory;

import org.apache.agila.bpel.client.AgilaEngine;
import org.apache.agila.bpel.engine.AgilaEngineFactory;
import org.apache.agila.bpel.engine.common.lifecycle.LifecycleManager;
import org.apache.agila.bpel.engine.common.persistence.FinderException;
import org.apache.agila.bpel.engine.common.persistence.XMLDataAccess;
import org.apache.agila.bpel.engine.common.transaction.TransactionManager;
import org.apache.agila.bpel.engine.priv.core.definition.AgilaProcess;
import org.apache.agila.bpel.engine.priv.core.definition.ProcessFactory;
import org.apache.agila.bpel.engine.priv.core.definition.impl.ProcessImpl;
import org.apache.agila.bpel.engine.priv.core.definition.impl.dao.ProcessDAO;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;
import org.dom4j.XPath;
import org.dom4j.io.DocumentResult;
import org.dom4j.io.SAXReader;
import org.mule.jbi.components.AbstractComponent;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Database;

/**
 * @author Propriétaire
 *
 */
public class AgilaComponent extends AbstractComponent {

	private static AgilaComponent instance;
	
	public static AgilaComponent getInstance() {
		return AgilaComponent.instance;
	}
	
	/* (non-Javadoc)
	 * @see org.mule.jbi.components.AbstractComponent#doInit()
	 */
	protected void doInit() throws Exception {
		// Set global instance
		AgilaComponent.instance = this;
		// Default initialization
		super.doInit();
		// Configure Xml database
		String workspaceRoot = this.context.getWorkspaceRoot();
		Database database = (Database) Class.forName("org.apache.xindice.client.xmldb.embed.DatabaseImpl").newInstance();
		database.setProperty("db-home", new File(workspaceRoot, "xindice").getCanonicalPath());
		database.setProperty("managed", "false");
		DatabaseManager.registerDatabase(database);
		// Create agila resources
		LifecycleManager.getLifecycleManager().createResources();
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.components.AbstractComponent#doShutDown()
	 */
	protected void doShutDown() throws Exception {
		// Default shutdown
		super.doShutDown();
		// Destroy agila resources
		LifecycleManager.getLifecycleManager().destroyResources();
		// Unset global instance
		AgilaComponent.instance = null;
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.components.AbstractComponent#doStart()
	 */
	protected void doStart() throws Exception {
		// Default start
		super.doStart();
		// Start agila resources
		LifecycleManager.getLifecycleManager().startResources();
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.components.AbstractComponent#doStop()
	 */
	protected void doStop() throws Exception {
		// Stop agila resources
		LifecycleManager.getLifecycleManager().stopResources();
		// Default stop
		super.doStop();
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.components.AbstractComponent#process(javax.jbi.messaging.MessageExchange)
	 */
	protected void process(MessageExchange me) {
		try {
			NormalizedMessage in = me.getMessage("in");
			DocumentResult result = new DocumentResult();
			TransformerFactory.newInstance().newTransformer().transform(in.getContent(), result);
			Document doc = result.getDocument();
			System.err.println(doc.asXML());
			String partner = me.getEndpoint().getEndpointName();
			String port = me.getInterfaceName().getLocalPart();
			String operation = me.getOperation().getLocalPart();
			AgilaEngineFactory.getEngine().acknowledge(partner, port, operation, doc);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.components.AbstractComponent#doDeploy(java.lang.String, java.lang.String)
	 */
	protected String doDeploy(String serviceUnitName, String serviceUnitRootPath) throws Exception {
		File suDir = new File(serviceUnitRootPath);
		if (!suDir.isDirectory()) {
			throw new DeploymentException("Given service unit path does not exists");
		}
		File bpel = new File(suDir, "process.xml");
		File wsdl = new File(suDir, "definition.xml");
		if (!bpel.isFile()) {
			throw new DeploymentException("Could not find process.xml");
		}
		if (!wsdl.isFile()) {
			throw new DeploymentException("Could not find definition.xml");
		}
		Document pbelDoc = new SAXReader().read(bpel);
		Document wsdlDoc = new SAXReader().read(wsdl);
		new AgilaDeployer().deploy(pbelDoc, wsdlDoc);
		return null;
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.components.AbstractComponent#doUndeploy(java.lang.String, java.lang.String)
	 */
	protected String doUndeploy(String serviceUnitName, String serviceUnitRootPath) throws Exception {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.components.AbstractComponent#doInit(java.lang.String, java.lang.String)
	 */
	protected void doInit(String serviceUnitName, String serviceUnitRootPath) throws Exception {
		//this.context.activateEndpoint(serviceName, portName);
		
//		// Register endpoints
//		File bpel = new File(serviceUnitRootPath, "process.xml");
//		File wsdl = new File(serviceUnitRootPath, "definition.xml");
//		Document bpelDoc = new SAXReader().read(bpel);
//		Document wsdlDoc = new SAXReader().read(wsdl);
//
//        XPath xpathSelector = DocumentHelper.createXPath("//*/bpel:receive");
//        HashMap nsMap = new HashMap(1);
//        nsMap.put("bpel", "http://schemas.xmlsoap.org/ws/2003/03/business-process/");
//        xpathSelector.setNamespaceURIs(nsMap);
//        List receivers = xpathSelector.selectNodes(bpelDoc);
//        for (Iterator iter = receivers.iterator(); iter.hasNext();) {
//			Node receiver = (Node) iter.next();
//			String partnerLink = receiver.valueOf("@partnerLink");
//			String portType = receiver.valueOf("@portType");
//			String operation = receiver.valueOf("@operation");
//			System.err.println("Receiver: partnerLink=" + partnerLink + ", portType=" + portType + ", operation=" + operation);
//		}
//      
		File wsdl = new File(serviceUnitRootPath, "definition.xml");
		DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
		f.setNamespaceAware(true);
		org.w3c.dom.Document wsdlDoc = f.newDocumentBuilder().parse(wsdl);
		Definition def = WSDLFactory.newInstance().newWSDLReader().readWSDL(null, wsdlDoc);
		for (Iterator it = def.getServices().keySet().iterator(); it.hasNext();) {
			QName serviceName = (QName) it.next();
			Service service = def.getService(serviceName);
			for (Iterator it2 = service.getPorts().keySet().iterator(); it2.hasNext();) {
				String portName = (String) it2.next();
				ServiceEndpoint se = this.context.activateEndpoint(serviceName, portName);
				setServiceDescription(se, wsdlDoc);
			}
		}
//		System.err.println(def.getExtensibilityElements());
	}

}

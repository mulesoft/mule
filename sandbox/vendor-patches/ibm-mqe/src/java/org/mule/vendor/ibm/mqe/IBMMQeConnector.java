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
package org.mule.vendor.ibm.mqe;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.providers.jms.JmsConnector;
import org.mule.umo.lifecycle.InitialisationException;

import com.ibm.mqe.MQeFields;
import com.ibm.mqe.MQeQueueManager;
import com.ibm.mqe.jms.MQeQueueConnectionFactory;
import com.ibm.mqe.registry.MQeRegistry;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class IBMMQeConnector extends JmsConnector {

	private String queueManagerName;

	private String baseDirectoryName;

	private MQeQueueManager mqeQMgr;

    public String getProtocol() {
        return "mqe";
    }

    public void doInitialise() throws InitialisationException {
    	setSpecification("1.0.2b");
	    try {
	    	startQueueManager();
	    } catch (Exception e) {
	    	throw new InitialisationException(
				       new Message(Messages.X_FAILED_TO_INITIALISE, "MQe connector"), e, this);
	    }
		setConnectionFactory(new MQeQueueConnectionFactory());
	    super.doInitialise();
	}

	/**
	 * This creates and starts the QueueManager from a pre-existing registry.
	 *
	 */
	public MQeQueueManager startQueueManager() throws Exception {
		logger.debug("Starting the queue manager.");

		// Create all the configuration information needed to construct the
		// queue manager in memory.
		MQeFields config = new MQeFields();

		// Construct the queue manager section parameters.
		MQeFields queueManagerSection = new MQeFields();

		queueManagerSection.putAscii(MQeQueueManager.Name, queueManagerName);
		config.putFields(MQeQueueManager.QueueManager, queueManagerSection);

		// Construct the registry section parameters.
		// In this examples, we use a public registry.
		MQeFields registrySection = new MQeFields();

		registrySection.putAscii(
			MQeRegistry.Adapter,
			"com.ibm.mqe.adapters.MQeDiskFieldsAdapter");
		registrySection.putAscii(
			MQeRegistry.DirName,
			baseDirectoryName + queueManagerName + "/Registry");

		config.putFields("Registry", registrySection);

		MQeQueueManager myQueueManager = new MQeQueueManager();

		myQueueManager.activate(config);
		logger.debug("Queue manager started.");
		return myQueueManager;
	}

	/**
	 * @return Returns the queueManager.
	 */
	public String getQueueManagerName() {
		return queueManagerName;
	}

	/**
	 * @param queueManager The queueManager to set.
	 */
	public void setQueueManagerName(String queueManager) {
		this.queueManagerName = queueManager;
	}

	/**
	 * @return Returns the baseDirectoryName.
	 */
	public String getBaseDirectoryName() {
		return baseDirectoryName;
	}

	/**
	 * @param baseDirectoryName The baseDirectoryName to set.
	 */
	public void setBaseDirectoryName(String baseDirectoryName) {
		this.baseDirectoryName = baseDirectoryName;
	}
}

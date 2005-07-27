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
package org.mule.jbi.messaging;

import org.apache.axis2.wsdl.builder.wsdl4j.WSDLPump;
import org.apache.wsdl.WSDLConstants;
import org.apache.wsdl.WSDLDescription;
import org.apache.wsdl.WSDLInterface;
import org.apache.wsdl.WSDLOperation;
import org.apache.wsdl.impl.WSDLDescriptionImpl;
import org.mule.jbi.registry.Component;
import org.mule.jbi.servicedesc.AbstractServiceEndpoint;
import org.w3c.dom.Document;

import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.InOptionalOut;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.RobustInOnly;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.wsdl.Definition;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 */
public class MessageExchangeFactoryImpl implements MessageExchangeFactory {

	public static final URI IN_ONLY_PATTERN = URI.create(WSDLConstants.MEP_URI_IN_ONLY);
	public static final URI IN_OPTIONAL_OUT_PATTERN = URI.create(WSDLConstants.MEP_URI_IN_OPTIONAL_OUT);
	public static final URI IN_OUT_PATTERN = URI.create(WSDLConstants.MEP_URI_IN_OUT);
	public static final URI ROBUST_IN_ONLY_PATTERN = URI.create(WSDLConstants.MEP_URI_ROBUST_IN_ONLY);
	
	private QName interfaceName;
	private QName service;
	private ServiceEndpoint endpoint;
	private DeliveryChannelImpl channel;
	
	public MessageExchangeFactoryImpl(DeliveryChannelImpl channel) {
		this.channel = channel;
	}
	
	public MessageExchange createExchange(QName serviceName, QName operationName) throws MessagingException {
		if (channel.isClosed()) {
			throw new MessagingException("Channel is closed");
		}
		ServiceEndpoint[] endpoints = this.channel.getContainer().getEndpoints().getInternalEndpointsForService(serviceName);
		Set meps = new HashSet();
		for (int i = 0; i < endpoints.length; i++) {
			try {
				// TODO: use axis builders when available
				// TODO: extract this code elsewhere
				// TODO: service descriptions should be cached
				// TODO: operations can be put on the endpoint 
				String name = ((AbstractServiceEndpoint) endpoints[i]).getComponent();
				Component info = this.channel.getContainer().getRegistry().getComponent(name);
				Document doc = info.getComponent().getServiceDescription(endpoints[i]);
				String uri = doc.getDocumentElement().getNamespaceURI();
				WSDLDescription desc = null; 
				if (WSDLConstants.WSDL2_0_NAMESPACE.equals(uri)) {
					throw new UnsupportedOperationException(uri);
				} else if (WSDLConstants.WSDL1_1_NAMESPACE.equals(uri)) {
			        WSDLReader reader = WSDLFactory.newInstance().newWSDLReader();
			        Definition def = reader.readWSDL(null, doc);
			        desc = new WSDLDescriptionImpl();
					WSDLPump pump = new WSDLPump(desc, def);
			        pump.pump();
				} else {
					throw new UnsupportedOperationException();
				}
				//WSDLService service = desc.getService(endpoints[i].getServiceName());
				//WSDLEndpoint endpoint = service.getEndpoint(new QName(service.getNamespace(), endpoints[i].getEndpointName()));
				Collection interfaces = desc.getWsdlInterfaces().values();
				for (Iterator iter = interfaces.iterator(); iter.hasNext();) {
					WSDLInterface itf = (WSDLInterface) iter.next();
					WSDLOperation op = itf.getOperation(operationName.getLocalPart());
					if (op != null) {
						meps.add(op.getMessageExchangePattern());
					}
				}
			} catch (Exception e) {
				// continue on
			}
		}
		if (meps.size() == 0) {
			throw new MessagingException("Could not determine mep");
		}
		if (meps.size() > 1) {
			throw new MessagingException("More than one mep for this operation");
		}
		MessageExchange me = createExchange(URI.create(meps.iterator().next().toString()));
		init(me);
		me.setService(serviceName);
		me.setOperation(operationName);
		return me;
	}

	public MessageExchange createExchange(URI pattern) throws MessagingException {
		if (channel.isClosed()) {
			throw new MessagingException("Channel is closed");
		}
		MessageExchange me;
		if (IN_ONLY_PATTERN.equals(pattern)) {
			me = new InOnlyImpl();
		} else if (IN_OPTIONAL_OUT_PATTERN.equals(pattern)) {
			me = new InOptionalOutImpl();
		} else if (IN_OUT_PATTERN.equals(pattern)) {
			me = new InOutImpl();
		} else if (ROBUST_IN_ONLY_PATTERN.equals(pattern)) {
			me = new RobustInOnlyImpl();
		} else {
			throw new MessagingException("Unsupported pattern");
		}
		init(me);
		return me;
	}

	public InOnly createInOnlyExchange() throws MessagingException {
		return (InOnly) createExchange(IN_ONLY_PATTERN);
	}

	public InOptionalOut createInOptionalOutExchange() throws MessagingException {
		return (InOptionalOut) createExchange(IN_OPTIONAL_OUT_PATTERN);
	}

	public InOut createInOutExchange() throws MessagingException {
		return (InOut) createExchange(IN_OUT_PATTERN);
	}

	public RobustInOnly createRobustInOnlyExchange() throws MessagingException {
		return (RobustInOnly) createExchange(ROBUST_IN_ONLY_PATTERN);
	}
	
	public static class InOnlyImpl extends MessageExchangeProxy implements InOnly {
		private static int[][] STATES_CONSUMER = {
			{ CAN_CONSUMER + CAN_OWNER + CAN_SET_IN_MSG + CAN_SEND + CAN_SEND_SYNC + CAN_STATUS_ACTIVE, 1, -1, -1 },
			{ CAN_CONSUMER, -1, -1, 2 },
			{ CAN_CONSUMER + CAN_OWNER, -1, -1, -1 },
		};
		private static int[][] STATES_PROVIDER = {
			{ CAN_PROVIDER, 1, -1, -1 },
			{ CAN_PROVIDER + CAN_OWNER + CAN_SEND + CAN_STATUS_DONE, -1, -1, 2 },
			{ CAN_PROVIDER, -1, -1, -1 },
		};
		public InOnlyImpl() {
			super(STATES_CONSUMER);
			this.me = new MessageExchangeImpl();
			this.twin = new InOnlyImpl(this);
		}
		protected InOnlyImpl(InOnlyImpl mep) {
			super(STATES_PROVIDER);
			this.me = mep.me;
			this.twin = mep;
		}
		public URI getPattern() {
			return IN_ONLY_PATTERN;
		}
		
	}

	public static class InOptionalOutImpl extends MessageExchangeProxy implements InOptionalOut {
		private static int[][] STATES_CONSUMER = {
			{ CAN_CONSUMER + CAN_OWNER + CAN_SET_IN_MSG + CAN_SEND + CAN_SEND_SYNC + CAN_STATUS_ACTIVE, 1, -1, -1},
			{ CAN_CONSUMER, 2, 2, 4 },
			{ CAN_CONSUMER + CAN_OWNER + CAN_SEND + CAN_STATUS_ERROR + CAN_STATUS_DONE, -1, 5, 3},
			{ CAN_CONSUMER, -1, -1, -1 },
			{ CAN_CONSUMER + CAN_OWNER, -1, -1, -1 },
			{ CAN_CONSUMER + CAN_OWNER, -1, -1, 3 },
		};
		private static int[][] STATES_PROVIDER = {
			{ CAN_PROVIDER, 1, -1, -1 },
			{ CAN_PROVIDER + CAN_OWNER + CAN_SET_OUT_MSG + CAN_SEND + CAN_STATUS_ACTIVE + CAN_STATUS_ERROR + CAN_STATUS_DONE, 2, 2, 4 },
			{ CAN_PROVIDER, -1, 5, 3 },
			{ CAN_PROVIDER + CAN_OWNER, -1, -1, -1 },
			{ CAN_PROVIDER, -1, -1, -1 },
			{ CAN_PROVIDER + CAN_OWNER + CAN_SEND + CAN_STATUS_DONE, -1, -1, 4 },
		};
		public InOptionalOutImpl() {
			super(STATES_CONSUMER);
			this.me = new MessageExchangeImpl();
			this.twin = new InOptionalOutImpl(this);
		}
		protected InOptionalOutImpl(InOptionalOutImpl mep) {
			super(STATES_PROVIDER);
			this.me = mep.me;
			this.twin = mep;
		}
		public URI getPattern() {
			return IN_OPTIONAL_OUT_PATTERN;
		}
	}

	public static class InOutImpl extends MessageExchangeProxy implements InOut {
		private static int[][] STATES_CONSUMER = {
			{ CAN_CONSUMER + CAN_OWNER + CAN_SET_IN_MSG + CAN_SEND + CAN_SEND_SYNC + CAN_STATUS_ACTIVE, 1, -1, -1},
			{ CAN_CONSUMER, 2, 2, -1 },
			{ CAN_CONSUMER + CAN_OWNER + CAN_SEND + CAN_STATUS_DONE, -1, -1, 3},
			{ CAN_CONSUMER, -1, -1, -1 },
		};
		private static int[][] STATES_PROVIDER = {
			{ CAN_PROVIDER, 1, -1, -1 },
			{ CAN_PROVIDER + CAN_OWNER + CAN_SET_OUT_MSG + CAN_SEND + CAN_SEND_SYNC + CAN_STATUS_ACTIVE + CAN_STATUS_ERROR, 2, 2, -1 },
			{ CAN_PROVIDER, -1, -1, 3 },
			{ CAN_PROVIDER + CAN_OWNER, -1, -1, -1 },
		};
		public InOutImpl() {
			super(STATES_CONSUMER);
			this.me = new MessageExchangeImpl();
			this.twin = new InOutImpl(this);
		}
		protected InOutImpl(InOutImpl mep) {
			super(STATES_PROVIDER);
			this.me = mep.me;
			this.twin = mep;
		}
		public URI getPattern() {
			return IN_OUT_PATTERN;
		}
	}

	public static class RobustInOnlyImpl extends MessageExchangeProxy implements RobustInOnly {
		private static int[][] STATES_CONSUMER = {
			{ CAN_CONSUMER + CAN_OWNER + CAN_SET_IN_MSG + CAN_SEND + CAN_SEND_SYNC + CAN_STATUS_ACTIVE, 1, -1, -1},
			{ CAN_CONSUMER, -1, 2, 4 },
			{ CAN_CONSUMER + CAN_OWNER + CAN_SEND + CAN_STATUS_DONE, -1, -1, 3},
			{ CAN_CONSUMER, -1, -1, -1 },
			{ CAN_CONSUMER + CAN_OWNER, -1, -1, -1 },
		};
		private static int[][] STATES_PROVIDER = {
			{ CAN_PROVIDER, 1, -1, -1 },
			{ CAN_PROVIDER + CAN_OWNER + CAN_SEND + CAN_STATUS_ERROR + CAN_STATUS_DONE, -1, 2, 4 },
			{ CAN_PROVIDER, -1, -1, 3 },
			{ CAN_PROVIDER + CAN_OWNER, -1, -1, -1 },
			{ CAN_PROVIDER, -1, -1, -1 },
		};
		public RobustInOnlyImpl() {
			super(STATES_CONSUMER);
			this.me = new MessageExchangeImpl();
			this.twin = new RobustInOnlyImpl(this);
		}
		protected RobustInOnlyImpl(RobustInOnlyImpl mep) {
			super(STATES_PROVIDER);
			this.me = mep.me;
			this.twin = mep;
		}
		public URI getPattern() {
			return ROBUST_IN_ONLY_PATTERN;
		}
	}

	public ServiceEndpoint getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(ServiceEndpoint endpoint) {
		this.endpoint = endpoint;
	}

	public QName getInterfaceName() {
		return interfaceName;
	}

	public void setInterfaceName(QName interfaceName) {
		this.interfaceName = interfaceName;
	}

	public QName getService() {
		return service;
	}

	public void setService(QName service) {
		this.service = service;
	}
	
	protected void init(MessageExchange me) {
		me.setInterfaceName(this.interfaceName);
		me.setEndpoint(this.endpoint);
		me.setService(this.service);
	}
}

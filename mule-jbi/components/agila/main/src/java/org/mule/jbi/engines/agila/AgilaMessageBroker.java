package org.mule.jbi.engines.agila;

import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.namespace.QName;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerFactory;

import org.apache.agila.bpel.engine.exception.EngineRuntimeException;
import org.apache.agila.bpel.engine.priv.core.definition.Activity;
import org.apache.agila.bpel.engine.priv.messaging.MessageBroker;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.io.DocumentResult;
import org.dom4j.io.DocumentSource;
import org.dom4j.io.SAXReader;

public class AgilaMessageBroker extends MessageBroker {

	/* (non-Javadoc)
	 * @see org.apache.agila.bpel.engine.priv.messaging.MessageBroker#asyncSend(org.apache.agila.bpel.engine.priv.core.definition.Activity, java.lang.String, java.lang.String, java.lang.String, org.dom4j.Document)
	 */
	protected void asyncSend(Activity sender, String partner, String portType, String operation, Document message) {
		try {
			AgilaComponent component = AgilaComponent.getInstance();
			if (component == null) {
				throw new IllegalStateException("AgilaComponent is not initialized");
			}
			DeliveryChannel channel = component.getContext().getDeliveryChannel();
			MessageExchangeFactory mef = channel.createExchangeFactory();
			MessageExchange me = mef.createInOnlyExchange();
			me.setInterfaceName(new QName(portType));
			me.setOperation(new QName(operation));
			NormalizedMessage in = me.createMessage();
			in.setContent(new DocumentSource(message));
			me.setMessage(in, "in");
			channel.send(me);
		} catch (Exception e) {
			throw new EngineRuntimeException(e);
		}
	}

	/* (non-Javadoc)
	 * @see org.apache.agila.bpel.engine.priv.messaging.MessageBroker#asyncSend(org.apache.agila.bpel.engine.priv.core.definition.Activity, java.lang.String, java.lang.String, java.lang.String, java.lang.String, org.dom4j.Document)
	 */
	protected void asyncSend(Activity sender, String partner, String namespace, String portType, String operation, Document message) {
		try {
			AgilaComponent component = AgilaComponent.getInstance();
			if (component == null) {
				throw new IllegalStateException("AgilaComponent is not initialized");
			}
			DeliveryChannel channel = component.getContext().getDeliveryChannel();
			MessageExchangeFactory mef = channel.createExchangeFactory();
			MessageExchange me = mef.createInOnlyExchange();
			me.setInterfaceName(new QName(namespace, portType));
			me.setOperation(new QName(namespace, operation));
			NormalizedMessage in = me.createMessage();
			in.setContent(new DocumentSource(message));
			me.setMessage(in, "in");
			channel.send(me);
		} catch (Exception e) {
			throw new EngineRuntimeException(e);
		}
	}

	/* (non-Javadoc)
	 * @see org.apache.agila.bpel.engine.priv.messaging.MessageBroker#syncSend(org.apache.agila.bpel.engine.priv.core.definition.Activity, java.lang.String, java.lang.String, java.lang.String, org.dom4j.Document)
	 */
	protected Document syncSend(Activity sender, String partner, String portType, String operation, Document message) {
		try {
			AgilaComponent component = AgilaComponent.getInstance();
			if (component == null) {
				throw new IllegalStateException("AgilaComponent is not initialized");
			}
			DeliveryChannel channel = component.getContext().getDeliveryChannel();
			MessageExchangeFactory mef = channel.createExchangeFactory();
			MessageExchange me = mef.createInOnlyExchange();
			me.setInterfaceName(new QName(portType));
			me.setOperation(new QName(operation));
			NormalizedMessage in = me.createMessage();
			in.setContent(new DocumentSource(message));
			me.setMessage(in, "in");
			channel.sendSync(me);
			NormalizedMessage out = me.getMessage("out");
			DocumentResult result = new DocumentResult();
			TransformerFactory.newInstance().newTransformer().transform(out.getContent(), result);
			return result.getDocument(); 
		} catch (Exception e) {
			throw new EngineRuntimeException(e);
		}
	}

	/* (non-Javadoc)
	 * @see org.apache.agila.bpel.engine.priv.messaging.MessageBroker#syncSend(org.apache.agila.bpel.engine.priv.core.definition.Activity, java.lang.String, java.lang.String, java.lang.String, java.lang.String, org.dom4j.Document)
	 */
	protected Document syncSend(Activity sender, String partner, String namespace, String portType, String operation, Document message) {
		try {
			AgilaComponent component = AgilaComponent.getInstance();
			if (component == null) {
				throw new IllegalStateException("AgilaComponent is not initialized");
			}
			DeliveryChannel channel = component.getContext().getDeliveryChannel();
			MessageExchangeFactory mef = channel.createExchangeFactory();
			MessageExchange me = mef.createInOnlyExchange();
			me.setInterfaceName(new QName(namespace, portType));
			me.setOperation(new QName(namespace, operation));
			NormalizedMessage in = me.createMessage();
			in.setContent(new DocumentSource(message));
			me.setMessage(in, "in");
			channel.sendSync(me);
			NormalizedMessage out = me.getMessage("out");
			DocumentResult result = new DocumentResult();
			TransformerFactory.newInstance().newTransformer().transform(out.getContent(), result);
			return result.getDocument(); 
		} catch (Exception e) {
			throw new EngineRuntimeException(e);
		}
	}

}

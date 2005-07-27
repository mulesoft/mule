package org.mule.jbi;
import com.fs.utils.DOMUtils;
import junit.framework.TestCase;
import org.mule.jbi.engines.pxe.PxeBootstrap;
import org.mule.jbi.engines.pxe.PxeComponent;
import org.mule.jbi.framework.JbiContainerImpl;
import org.mule.jbi.registry.Engine;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import java.io.File;
import java.net.URI;
import java.net.URL;

public class PxeJbiTestCase extends TestCase {

	public static final QName SERVICE_NAME = new QName("http://jbi.mule.org", "myService");
	public static final String ENDPOINT_NAME = "myEndpoint";

	public void test() throws Exception {
		JbiContainerImpl container = new JbiContainerImpl();
		container.setWorkingDir(new File("target/.mule-jbi"));
		// Initialize jbi
		container.initialize();
		// Create components
		PxeComponent provider = new PxeComponent();
		TestComponent consumer1 = new TestComponent();
		TestComponent consumer2 = new TestComponent();
		// Register components
		container.getRegistry().addTransientEngine("consumer1", consumer1);
		container.getRegistry().addTransientEngine("consumer2", consumer2);
		Engine pxe = container.getRegistry().addTransientEngine("provider", provider, new PxeBootstrap());
		// Start jbi
		container.start();
		// Deploy service unit
		//URL url = Thread.currentThread().getContextClassLoader().getResource("AsyncProcess/AsyncProcess.bpel");
		URL url = Thread.currentThread().getContextClassLoader().getResource("AsyncProcessSU/AsyncProcess.jar");
		File asyncProcessDir = new File(URI.create(url.toExternalForm())).getParentFile();
		container.getRegistry().addTransientUnit("AsyncProcess", pxe, asyncProcessDir.getAbsolutePath());

		// Register endpoints
		consumer2.getContext().activateEndpoint(new QName("uri:com.bptest.process", "ResponderSVC"), "ResponderPORT");
		// Send message exchange
		ServiceEndpoint se = consumer1.getContext().getEndpoint(new QName("uri:com.bptest.process", "ProcessSVC"), "ProcessPORT");
		MessageExchangeFactory mef = consumer1.getChannel().createExchangeFactory(se);
		InOnly me = mef.createInOnlyExchange();
		me.setOperation(new QName("uri:com.bptest.process", "Run"));
		NormalizedMessage m = me.createMessage();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        org.w3c.dom.Document soap = dbf.newDocumentBuilder().parse(Thread.currentThread().getContextClassLoader().getResourceAsStream("message.xml"));
		m.setContent(new DOMSource(soap.getDocumentElement()));
		me.setInMessage(m);
		
		// Set transaction context
		//container.getTransactionManager().begin();
		//me.setProperty(me.JTA_TRANSACTION_PROPERTY_NAME, container.getTransactionManager().getTransaction());
		consumer1.getChannel().send(me);
		assertNotNull(consumer1.getChannel().accept(5000));
		
		MessageExchange me2 = consumer2.getChannel().accept(5000);
		assertNotNull(me2);
		DOMResult r = new DOMResult();
		TransformerFactory.newInstance().newTransformer().transform(me2.getMessage("in").getContent(), r);
		String xml = DOMUtils.domToString(r.getNode());
		System.err.println(xml);
		me2.setStatus(ExchangeStatus.DONE);
		consumer2.getChannel().send(me2);
	}
	
}

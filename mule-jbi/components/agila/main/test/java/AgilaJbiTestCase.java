import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.DocumentSource;
import org.mule.jbi.TestComponent;
import org.mule.jbi.engines.agila.AgilaBootstrap;
import org.mule.jbi.engines.agila.AgilaComponent;
import org.mule.jbi.framework.JbiContainerImpl;
import org.mule.jbi.registry.Engine;


public class AgilaJbiTestCase extends TestCase {

	public static final QName SERVICE_NAME = new QName("http://jbi.mule.org", "myService");
	public static final String ENDPOINT_NAME = "myEndpoint";

	public void test() throws Exception {
		JbiContainerImpl container = new JbiContainerImpl();
		container.setWorkingDir(new File("target/.mule-jbi"));
		// Initialize jbi
		container.initialize();
		// Create components
		AgilaComponent provider = new AgilaComponent();
		TestComponent consumer = new TestComponent();
		// Register components
		container.getRegistry().addTransientEngine("consumer", consumer);
		Engine agila = container.getRegistry().addTransientEngine("provider", provider, new AgilaBootstrap());
		// Start jbi
		container.start();
		// Deploy service unit
		URL url = Thread.currentThread().getContextClassLoader().getResource("loanbroker/process.xml");
		File loanbroker = new File(url.toURI()).getParentFile();
		container.getRegistry().addTransientUnit("loanbroker", agila, loanbroker.getAbsolutePath());
		// Send message exchange
		MessageExchangeFactory mef = consumer.getChannel().createExchangeFactory();
		InOnly me = mef.createInOnlyExchange();
		me.setInterfaceName(new QName("http://www.apache.org/agila/samples/exemple1/definition/", "loanService"));
		me.setOperation(new QName("http://www.apache.org/agila/samples/exemple1/definition/", "request"));
		NormalizedMessage m = me.createMessage();
        Document doc = DocumentHelper.createDocument();
        Element msg = doc.addElement("message");
        msg.addElement("firstName").setText("john");
        msg.addElement("lastName").setText("doe");
        msg.addElement("amount").setText("5000");
		m.setContent(new DocumentSource(msg));
		me.setInMessage(m);
		consumer.getChannel().send(me);
	}
	
}

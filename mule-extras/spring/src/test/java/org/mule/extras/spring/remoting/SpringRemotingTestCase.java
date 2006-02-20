package org.mule.extras.spring.remoting;

import org.mule.tck.FunctionalTestCase;
import org.springframework.remoting.httpinvoker.HttpInvokerProxyFactoryBean;

public class SpringRemotingTestCase extends FunctionalTestCase
{
    private static final String SPRING_ENDPOINT = "http://localhost:8003/springService";


    protected String getConfigResources() {
        return "spring-remoting-mule-config.xml";
    }

    public void testInvokeSpringService() throws Exception
    {
        ComplexData cd = new ComplexData("Foo", new Integer(13));
        HttpInvokerProxyFactoryBean invoker = new HttpInvokerProxyFactoryBean();
        invoker.setServiceInterface(WorkInterface.class);
        invoker.setServiceUrl(SPRING_ENDPOINT);
        invoker.afterPropertiesSet();
        WorkInterface worker = (WorkInterface) invoker.getObject();
        ComplexData data = worker.executeComplexity(cd);
        assertNotNull(data);
        assertEquals(data.getSomeString(), "Foo Received");
        assertEquals(data.getSomeInteger(), new Integer(14));
    }
}

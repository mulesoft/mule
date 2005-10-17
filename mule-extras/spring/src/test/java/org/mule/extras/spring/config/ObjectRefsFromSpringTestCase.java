package org.mule.extras.spring.config;

import org.mule.MuleManager;
import org.mule.providers.AbstractConnector;
import org.mule.providers.SimpleRetryConnectionStrategy;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.testmodels.mule.TestCompressionTransformer;
import org.mule.tck.testmodels.mule.TestExceptionStrategy;
import org.mule.umo.endpoint.UMOEndpoint;

public class ObjectRefsFromSpringTestCase extends FunctionalTestCase {

        protected String getConfigResources() {
            return "test-refs-from-spring.xml";
        }

    public void testObjectCreation() throws Exception
    {
        UMOEndpoint ep = MuleManager.getInstance().lookupEndpoint("foo");
        assertNotNull(ep);
        assertEquals("testConnector", ep.getConnector().getName());
        assertTrue(((AbstractConnector)ep.getConnector()).getConnectionStrategy() instanceof SimpleRetryConnectionStrategy);
        assertTrue(((AbstractConnector)ep.getConnector()).getExceptionListener() instanceof TestExceptionStrategy);

        assertNotNull(ep.getTransformer());
        assertEquals("testTransformer", ep.getTransformer().getName());
        assertTrue(ep.getTransformer() instanceof TestCompressionTransformer);
        assertEquals(12, ((TestCompressionTransformer)ep.getTransformer()).getBeanProperty2());
        
    }
}

package org.mule.tck.external.services.servlet.tomcat;

import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import junit.framework.TestCase;

public class TomcatServiceTestCase extends TestCase
{
    private final Log logger = LogFactory.getLog(getClass());
    private TomcatService service = null;

    protected void setUp()
    {
        service = new TomcatService();
        service.setBaseDir("c:/test-tomcat");
    }

    public void testStart() throws Exception
    {
        service.init();
        service.deployWarFile("hello", new URL("file:C:/hello.war"));
        service.start();
    }

    public void testStop() throws Exception
    {
        service.stop();
    }

}

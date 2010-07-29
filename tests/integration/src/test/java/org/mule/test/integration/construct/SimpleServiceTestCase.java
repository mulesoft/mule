
package org.mule.test.integration.construct;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.mule.api.MuleException;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

public class SimpleServiceTestCase extends FunctionalTestCase
{

    private MuleClient muleClient;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        muleClient = new MuleClient(muleContext);
    }

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/construct/simple-service-config.xml";
    }

    public void testPureAttributes() throws Exception
    {
        doTestMathsService("vm://maths1.in");
    }

    public void testAbstractInheritence() throws Exception
    {
        doTestMathsService("vm://maths2.in");
    }

    public void testEndpointReference() throws Exception
    {
        doTestMathsService("vm://maths3.in");
    }

    public void testComponentReference() throws Exception
    {
        doTestMathsService("vm://maths4.in");
    }

    public void testChildComponent() throws Exception
    {
        doTestMathsService("vm://maths5.in");
    }

    public void testComponentWithEntryPointResolver() throws Exception
    {
        doTestMathsService("vm://maths6.in");
    }

    public void testTransformerReferences() throws Exception
    {
        doTestStringMassager("vm://bam1.in");
    }

    public void testConcreteInheritence() throws Exception
    {
        doTestStringMassager("vm://bam2.in");
    }

    private void doTestMathsService(String url) throws MuleException
    {
        final int a = RandomUtils.nextInt(100);
        final int b = RandomUtils.nextInt(100);
        final int result = (Integer) muleClient.send(url, new int[]{a, b}, null).getPayload();
        assertEquals(a + b, result);
    }

    private void doTestStringMassager(String url) throws Exception, MuleException
    {
        final String s = RandomStringUtils.randomAlphabetic(10);
        final String result = muleClient.send(url, s.getBytes(), null).getPayloadAsString();
        assertEquals(s + "barbaz", result);
    }
}

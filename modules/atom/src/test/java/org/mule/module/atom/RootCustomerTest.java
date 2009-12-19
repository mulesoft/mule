package org.mule.module.atom;


public class RootCustomerTest extends AbstractCustomerTest
{

    @Override
    protected String getConfigResources()
    {
        return "customer-conf.xml";
    }

    public void testCustomerProvider() throws Exception
    {
        testCustomerProvider("");
    }
}

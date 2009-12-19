package org.mule.module.atom;


public class NonRootCustomerTest extends AbstractCustomerTest
{

    @Override
    protected String getConfigResources()
    {
        return "customer-nonroot-conf.xml";
    }

    public void testCustomerProvider() throws Exception
    {
        testCustomerProvider("/base");
    }
}

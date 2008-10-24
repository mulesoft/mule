package org.mule.management;

import org.mule.management.stats.ServiceStatistics;
import org.mule.tck.FunctionalTestCase;
import org.mule.module.client.MuleClient;

public class JmxStatisticsAsyncTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "jmx-statistics-test.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        MuleClient muleClient = new MuleClient(FunctionalTestCase.muleContext);
        muleClient.dispatch("vm://in", "Hello world", null);
        muleClient.request("vm://out", 5000);
    }

    @Override
    protected void doTearDown() throws Exception
    {
        getServiceStatistics().clear();
        super.doTearDown();
    }

    public void testCorrectAverageQueueSize() throws Exception
    {
        ServiceStatistics stats = getServiceStatistics();
        assertEquals(1, stats.getAverageQueueSize());
    }

    public void testCorrectAsynchEventsReceived() throws Exception
    {
        ServiceStatistics stats = getServiceStatistics();
        assertEquals(1, stats.getAsyncEventsReceived());
    }

    public void testCorrectMaxQueueSize() throws Exception
    {
        ServiceStatistics stats = getServiceStatistics();
        assertEquals(1, stats.getMaxQueueSize());
    }

    public void testCorrectAsynchEventsSent() throws Exception
    {
        ServiceStatistics stats = getServiceStatistics();
        assertEquals(1, stats.getAsyncEventsSent());
    }

    public void testCorrectTotalEventsSent() throws Exception
    {
        ServiceStatistics stats = getServiceStatistics();
        assertEquals(1, stats.getTotalEventsSent());
    }

    public void testCorrectTotalEventsReceived() throws Exception
    {
        ServiceStatistics stats = getServiceStatistics();
        assertEquals(1, stats.getTotalEventsReceived());
    }

    private ServiceStatistics getServiceStatistics()
    {
        return (ServiceStatistics) muleContext.getStatistics().getComponentStatistics().iterator().next();
    }

}

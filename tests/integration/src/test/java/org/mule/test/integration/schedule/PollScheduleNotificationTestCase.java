package org.mule.test.integration.schedule;


import org.mule.api.context.notification.EndpointMessageNotificationListener;
import org.mule.context.notification.EndpointMessageNotification;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class PollScheduleNotificationTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/schedule/poll-notifications-config.xml";
    }

    @Test
    public void test() throws InterruptedException
    {
        System.out.println("tito");
        Thread.sleep(5000);
        muleContext.getNotificationManager().addListener(new MyListener());
        Thread.sleep(5000);
    }
    class MyListener implements EndpointMessageNotificationListener<EndpointMessageNotification>{

        @Override
        public void onNotification(EndpointMessageNotification notification)
        {
            System.out.println("fefe");
        }
    }
}

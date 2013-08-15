package org.mule.test.integration.schedule;


import static junit.framework.Assert.assertEquals;
import org.mule.api.AnnotatedObject;
import org.mule.api.context.notification.EndpointMessageNotificationListener;
import org.mule.context.notification.EndpointMessageNotification;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.junit.Test;

public class PollScheduleNotificationTestCase extends FunctionalTestCase
{

    public static final QName NAME = new QName("http://www.mulesoft.org/schema/mule/documentation", "name");

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/schedule/poll-notifications-config.xml";
    }

    @Test
    public void validateNotificationsAreSent() throws InterruptedException
    {
        MyListener listener = new MyListener();
        muleContext.getNotificationManager().addListener(listener);
        Thread.sleep(5000);

        assertEquals("pollName", listener.getNotifications().get(0));

    }
    class MyListener implements EndpointMessageNotificationListener<EndpointMessageNotification>{

        List<String> notifications = new ArrayList<String>();

        @Override
        public void onNotification(EndpointMessageNotification notification)
        {
            notifications.add((String) ((AnnotatedObject)notification.getImmutableEndpoint()).getAnnotation(NAME));
        }

        public List<String> getNotifications()
        {
            return notifications;
        }
    }
}

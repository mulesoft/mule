package org.mule.module.atom.event;

import org.apache.abdera.model.Feed;

public class EventReceiver //implements Callable
{

    public static int receivedEntries = 0;

    // TODO: why doesn't this signature work?
    public void receive(Feed feed) throws Exception
    {

//    public Object onCall(MuleEventContext ctx) throws Exception {
//        Feed feed = ctx.getMessage().getPayload(Feed.class);
        System.out.println("Received " + feed.getEntries().size() + " events");

        receivedEntries = feed.getEntries().size();

        //return null;
    }
}

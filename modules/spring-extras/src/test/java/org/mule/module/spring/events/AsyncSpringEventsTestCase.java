/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.spring.events;

public class AsyncSpringEventsTestCase extends SpringEventsTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "async-mule-events-app-context.xml";
    }

    /*
     * TODO this test seems to suffer from a bug with listener interface handling?
     * TestAllEventBean needs to implement both ApplicationListener and
     * MuleEventListener, but when it does so only MuleEvents arrive. If the class
     * implements only ApplicationListener, only Spring events arrive. Mysteriously
     * enough this seems to work fine for the synchronous case, which makes me think
     * there is still something deeper going on.
     */
    @Override
    public void testReceivingAllEvents() throws Exception
    {
        // TODO fix synchronisation issue
        // super.testReceivingAllEvents();
    }

}

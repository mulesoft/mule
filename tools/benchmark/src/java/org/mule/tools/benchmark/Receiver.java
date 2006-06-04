/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.tools.benchmark;

import org.mule.config.builders.QuickConfigurationBuilder;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOException;

import java.util.HashMap;
import java.util.Map;

/**
 * <code>Receiver</code> receives messages according to the Runner configuration
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class Receiver extends Runner // implements Callable
{
    public static void main(String[] args)
    {
        try
        {
            Receiver receiver = new Receiver(new RunnerConfig(args));
            receiver.register();
            receiver.start();

        } catch (Throwable e)
        {
            e.printStackTrace();
        }
    }

    public Receiver(RunnerConfig config) throws Exception
    {
        super(config);
    }

    public void register() throws UMOException
    {
        QuickConfigurationBuilder builder = new QuickConfigurationBuilder();

        builder.setModel(config.getModel());
        String endpoint = config.getEndpointsArray()[config.getEndpointsArray().length-1];
        EventCallback callback = new EventCallback()
        {
            public void eventReceived(UMOEventContext context, Object Component) throws Exception
            {
                String msg = context.getTransformedMessageAsString();
                if(counter > 0 & (counter % 1000 == 0)) System.out.println("Received " + counter + " on: " + context.getEndpointURI().toString());
                count(1);
            }
        };

        Map properties  = new HashMap();
        properties.put("eventCallback", callback);

        builder.registerComponent(FunctionalTestComponent.class.getName(), "Benchmark Receiver", new MuleEndpointURI(endpoint), properties);
    }

//    public Object onCall(UMOEventContext eventContext) throws Exception
//    {
//        String msg = eventContext.getTransformedMessageAsString();
//        if(counter > 0 & (counter % 1000 == 0)) System.out.println("Received " + counter + " on: " + eventContext.getEndpointURI().toString());
//        count(1);
//        return null;
//    }
}

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

import org.mule.MuleException;
import org.mule.MuleManager;
import org.mule.config.PoolingProfile;
import org.mule.config.QueueProfile;
import org.mule.config.ThreadingProfile;
import org.mule.config.builders.QuickConfigurationBuilder;
import org.mule.extras.client.MuleClient;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.service.ConnectorFactory;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.EndpointException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.manager.UMOManager;

import java.util.Arrays;

/**
 * <code>Sender</code> sends messages according to the Runner configuration
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class Sender extends Runner {
    public static void main(String[] args) {
        try {
            Sender sender = new Sender(new RunnerConfig(args));
            sender.start();
            sender.send();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public Sender(RunnerConfig config) throws Exception {
        super(config);
        init();
    }

    protected void init() throws UMOException {
        QuickConfigurationBuilder builder = new QuickConfigurationBuilder(true);
        builder.setModel(config.getModel());
        ThreadingProfile tp = new ThreadingProfile(config.getConnectorThreads(), config.getConnectorThreads(), -1, (byte) 2, null, null);
        MuleManager.getConfiguration().setMessageReceiverThreadingProfile(tp);
        MuleManager.getConfiguration().setMessageDispatcherThreadingProfile(tp);

        UMOManager manager = builder.createStartedManager(config.isSynchronous(), "");
        manager.stop();

        if (config.getEndpointsArray().length > 1) {

            int j = 1;
            String in;
            String out;
            String[] endpoints = config.getEndpointsArray();
            for (int i = 0; i < endpoints.length - 1; i++) {
                in = endpoints[i];
                if ((i + 1) <= endpoints.length) {
                    out = endpoints[i + 1];
                } else {
                    break;
                }
                MuleDescriptor d = createDescriptor("benchmark" + j, in, out);
                builder.registerComponent(d);
                j++;
            }
        }
    }

    protected MuleDescriptor createDescriptor(String name, String in, String out) throws EndpointException, MuleException {
        UMOEndpointURI inbound = null;
        UMOEndpointURI outbound = null;
        if (in != null) inbound = new MuleEndpointURI(in);
        if (out != null) outbound = new MuleEndpointURI(out);

        MuleDescriptor d = new MuleDescriptor(name);
        d.setImplementation(BenchmarkComponent.class.getName());
        d.setThreadingProfile(new ThreadingProfile(config.getThreads(), config.getThreads(), -1, (byte) 4, null, null));
        d.setQueueProfile(new QueueProfile(config.getQueue(), false));
        d.setPoolingProfile(new PoolingProfile(config.getThreads(), config.getThreads(), 0, (byte) 2, PoolingProfile.POOL_INITIALISE_ALL_COMPONENTS));
        d.setInboundEndpoint(ConnectorFactory.createEndpoint(inbound, UMOEndpoint.ENDPOINT_TYPE_RECEIVER));

        if (outbound != null) {
            d.setOutboundEndpoint(ConnectorFactory.createEndpoint(outbound, UMOEndpoint.ENDPOINT_TYPE_SENDER));
        }
        return d;
    }

    public void send() throws UMOException {
        byte[] msg = new byte[config.getMessageSize()];
        Arrays.fill(msg, (byte) 0);
        String message = new String(msg);

        MuleClient client = new MuleClient();
        String endpoint = config.getEndpointsArray()[0];

        System.out.println("Starting sender on : " + endpoint);
        System.out.println("Message length: " + message.length());
        for (int i = 0; i < config.getMessages(); i++) {
            client.dispatch(endpoint, message, null);
            count(1);
        }
        printReport();
    }

    protected void printReport() {
        System.out.println("Sent : " + config.getMessages() + " Messages");
    }
}

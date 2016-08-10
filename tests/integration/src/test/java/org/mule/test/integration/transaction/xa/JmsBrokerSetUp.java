/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transaction.xa;

import com.google.common.collect.Lists;

import java.util.List;

import org.apache.activemq.broker.BrokerPlugin;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.security.AuthenticationUser;
import org.apache.activemq.security.SimpleAuthenticationPlugin;

public class JmsBrokerSetUp implements TransactionalTestSetUp
{

    private final int port;
    private BrokerService broker;
    private final List<AuthenticationUser> users = Lists.newArrayList();

    public JmsBrokerSetUp(int port)
    {
        this.port = port;
    }

    @Override
    public void initialize() throws Exception
    {
        broker = new BrokerService();
        broker.setUseJmx(false);
        broker.setPersistent(false);
        broker.addConnector("tcp://localhost:" + port);

        if (!users.isEmpty())
        {
            SimpleAuthenticationPlugin authenticationPlugin = new SimpleAuthenticationPlugin(users);
            broker.setPlugins(new BrokerPlugin[] {authenticationPlugin});
        }
        broker.start();
    }

    @Override
    public void finalice() throws Exception
    {
        try
        {
            broker.stop();
        }
        catch (Exception e)
        {
        }
    }

    public void addUser(String username, String password, String groups)
    {
        users.add(new AuthenticationUser(username, password, groups));
    }
}

/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.echo;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.service.test.api.EchoService;

public class DefaultEchoService implements EchoService, Startable, Stoppable {
    public DefaultEchoService() {
    }

    public String echo(String message) {
        return message;
    }

    public void start() throws MuleException {
        System.out.println("Starting " + this.getClass().getName());
    }

    public void stop() throws MuleException {
        System.out.println("Stopping " + this.getClass().getName());
    }

    public String getName() {
        return "DefaultEchoService";
    }
}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

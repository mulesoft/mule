/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf.jaxws;

import org.mule.tck.probe.Probe;

import org.apache.hello_world_soap_http.GreeterImpl;

public class GreeterNotNull implements Probe
{
    private GreeterImpl impl;

    public GreeterNotNull(GreeterImpl impl)
    {
        this.impl = impl;
    }

    public boolean isSatisfied()
    {
        return impl != null;
    }

    public String describeFailure()
    {
        return "Expected Greeter implementation but found null";
    }

}

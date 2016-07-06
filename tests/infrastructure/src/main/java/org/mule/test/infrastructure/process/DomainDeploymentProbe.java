/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure.process;

import org.mule.tck.probe.Probe;

public class DomainDeploymentProbe implements Probe
{

    private boolean check;
    private MuleProcessController mule;
    private String domainName;

    public static DomainDeploymentProbe isDeployed(MuleProcessController mule, String domainName)
    {
        return new DomainDeploymentProbe(mule, domainName, true);
    }

    public static DomainDeploymentProbe notDeployed(MuleProcessController mule, String domainName)
    {
        return new DomainDeploymentProbe(mule, domainName, false);
    }

    protected DomainDeploymentProbe(MuleProcessController mule, String domainName, Boolean check)
    {
        this.mule = mule;
        this.domainName = domainName;
        this.check = check;
    }

    public boolean isSatisfied()
    {
        return check == mule.isDomainDeployed(domainName);
    }

    public String describeFailure()
    {
        return "Domain [" + domainName + "] is " + (check ? "not" : "") + " deployed.";
    }
}

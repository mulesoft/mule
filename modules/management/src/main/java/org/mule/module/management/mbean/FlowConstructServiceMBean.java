/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.management.mbean;

import javax.management.ObjectName;

/**
 * <code>FlowConstructServiceMBean</code> defines the management interface for a mule
 * managed flow construct.
 */
public interface FlowConstructServiceMBean  extends FlowConstructStatsMBean
{
    /**
     * The statistics for this flow construct
     *
     * @return statistics for this flow construct
     * @see org.mule.module.management.mbean.FlowConstructStats
     */
    ObjectName getStatistics();

    /**
     * The name of this service
     *
     * @return The name of this service
     */
    String getName();

    /**
     * The type of flow construct
     */
    String getType();
}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

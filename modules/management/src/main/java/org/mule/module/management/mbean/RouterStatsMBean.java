/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.management.mbean;

import java.util.Map;

/**
 * <code>RouterStatsMBean</code> TODO
 */
public interface RouterStatsMBean
{

    long getCaughtMessages();

    long getNotRouted();

    long getTotalReceived();

    long getTotalRouted();

    Map getRouted();
}

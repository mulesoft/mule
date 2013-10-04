/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.quartz.jobs;

import org.quartz.StatefulJob;

/**
 * Same as {@link org.mule.transport.quartz.jobs.EndpointPollingJob} except the JobDetail state is persistent
 * for each request and only one instance of the job will fire  at any given trigger. If the job does not
 * complete before the next trigger the second execution is blocked until the job completes
 */
public class StatefulEndpointPollingJob extends EndpointPollingJob implements StatefulJob
{
}

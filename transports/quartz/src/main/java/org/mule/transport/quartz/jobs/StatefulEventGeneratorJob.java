/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.quartz.jobs;

import org.quartz.StatefulJob;

/**
 * Same as {@link org.mule.transport.quartz.jobs.EventGeneratorJob} except the JobDetail state is persistent
 * for each request and only one instance of the job will fire  at any given trigger. If the job does not
 * complete before the next trigger the second execution is blocked until the job completes
 */
public class StatefulEventGeneratorJob extends EventGeneratorJob implements StatefulJob
{
}

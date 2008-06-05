/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.bpm.jbpm;

import org.apache.commons.lang.NotImplementedException;
import org.jbpm.JbpmContext;
import org.jbpm.job.Job;


/**
 * This is an unfinished work, see MULE-1219
 */
public class MuleMessage extends Job
{
    private static final long serialVersionUID = 1L;

    public boolean execute(JbpmContext jbpmContext) throws Exception
    {
        throw new NotImplementedException("MULE-1219");
    }
}

/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.bpm.jbpm.actions;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;

/**
 * Simply continues the process execution (moves on to the next state).
 */
public class Continue implements ActionHandler
{

    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception
    {
        executionContext.leaveNode();
    }

}

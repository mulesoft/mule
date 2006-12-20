/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.bpm;

import java.util.Map;

public interface BPMS
{

    /**
     * Set a callback to generate messages within Mule.
     * 
     * @param msgService An interface within Mule which the BPMS may call to generate
     *            Mule messages.
     */
    public void setMessageService(MessageService msgService);

    /**
     * Start a new process.
     * 
     * @param processType - the type of process to start
     * @param processVariables - optional process variables/parameters to set
     * @return an object representing the new process
     */
    public Object startProcess(Object processType, Map processVariables) throws Exception;

    /**
     * Advance an already-running process.
     * 
     * @param processId - an ID which identifies the running process
     * @param transition - optionally specify which transition to take from the
     *            current state
     * @param processVariables - optional process variables/parameters to set
     * @return an object representing the process in its new (i.e., advanced) state
     */
    public Object advanceProcess(Object processId, Object transition, Map processVariables) throws Exception;

    /**
     * Update the variables/parameters for an already-running process.
     * 
     * @param processId - an ID which identifies the running process
     * @param processVariables - process variables/parameters to set
     * @return an object representing the process in its new (i.e., updated) state
     */
    public Object updateProcess(Object processId, Map processVariables) throws Exception;

    /**
     * Abort (end abnormally) a running process.
     * 
     * @param processId - an ID which identifies the running process
     */
    public void abortProcess(Object processId) throws Exception;

    /**
     * Looks up an already-running process.
     * 
     * @return an object representing the process
     */
    public Object lookupProcess(Object processId) throws Exception;

    /**
     * @return an ID which identifies the given process.
     */
    public Object getId(Object process) throws Exception;

    /**
     * @return the current state of the given process.
     */
    public Object getState(Object process) throws Exception;

    /**
     * @return true if the given process has ended.
     */
    public boolean hasEnded(Object process) throws Exception;

    /**
     * @return true if the object is a valid process
     */
    public boolean isProcess(Object obj) throws Exception;

}

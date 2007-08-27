/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.bpm;

import java.util.Map;

/**
 * A generic interface for any Process Engine.  
 * 
 * Theoretically, any Process Engine can be "plugged into" Mule via the BPM connector
 * if it implements this interface.  
 * 
 * @see {@link MessageService}
 */
public interface BPMS
{
    /**
     * {@link MessageService} contains a callback method used to generate Mule messages from your process.
     * This method is REQUIRED.
     * 
     * @param msgService An interface within Mule which the BPMS may call to generate
     *            Mule messages.
     */
    public void setMessageService(MessageService msgService);

    /**
     * Start a new process.
     * This method is REQUIRED.
     * 
     * @param processType - the type of process to start
     * @param processVariables - optional process variables/parameters to set
     * @return an object representing the new process
     */
    public Object startProcess(Object processType, Object transition, Map processVariables) throws Exception;

    /**
     * Advance an already-running process.
     * This method is REQUIRED.
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
     * This method is OPTIONAL.
     *
     * @param processId - an ID which identifies the running process
     * @param processVariables - process variables/parameters to set
     * @return an object representing the process in its new (i.e., updated) state
     */
    public Object updateProcess(Object processId, Map processVariables) throws Exception;

    /**
     * Abort a running process (end abnormally).
     * This method is OPTIONAL.
     * 
     * @param processId - an ID which identifies the running process
     */
    public void abortProcess(Object processId) throws Exception;

    /**
     * Looks up an already-running process.
     * This method is OPTIONAL.
     * 
     * @return an object representing the process
     */
    public Object lookupProcess(Object processId) throws Exception;

    /**
     * @return an ID which identifies the given process.
     * This method is OPTIONAL.
     */
    public Object getId(Object process) throws Exception;

    /**
     * @return the current state of the given process.
     * This method is OPTIONAL.
     */
    public Object getState(Object process) throws Exception;

    /**
     * @return true if the given process has ended.
     * This method is OPTIONAL.
     */
    public boolean hasEnded(Object process) throws Exception;

    /**
     * @return true if the object is a valid process
     * This method is OPTIONAL.
     */
    public boolean isProcess(Object obj) throws Exception;
}

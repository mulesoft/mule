/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.transaction;

import org.mule.transaction.constraints.ConstraintFilter;

/**
 * <code>TransactionConfig</code> defines transaction configuration for a
 * transactional endpoint.
 */
public interface TransactionConfig
{
    /** 
     * Whether there is a transaction available or not, ignore it 
     * <p>
     * J2EE: NotSupported
     */
    byte ACTION_NONE = 0;

    /** 
     * Will ensure that a new transaction is created for each invocation 
     * <p>
     * J2EE RequiresNew
     */
    byte ACTION_ALWAYS_BEGIN = 1;

    /** 
     * Will begin a new transaction if no transaction is already present 
     * <p>
     * J2EE: Required
     */
    byte ACTION_BEGIN_OR_JOIN = 2;

    /** 
     * There must always be a transaction present for the invocation 
     * <p>
     * J2EE: Mandatory
     */
    byte ACTION_ALWAYS_JOIN = 3;

    /** 
     * If there is a transaction available, then use it, otherwise continue processing 
     * <p>
     * J2EE: Supports
     */
    byte ACTION_JOIN_IF_POSSIBLE = 4;

    /**
     * There must not be a transaction present for the invocation
     * <p>
     * J2EE Never
     */
    byte ACTION_NEVER = 5;

    /**
     * Transaction action by default
     * <p>
     */
    byte ACTION_DEFAULT = ACTION_NEVER;
    
    TransactionFactory getFactory();

    void setFactory(TransactionFactory factory);

    byte getAction();

    void setAction(byte action);

    boolean isTransacted();

    ConstraintFilter getConstraint();

    void setConstraint(ConstraintFilter constraint);

    void setTimeout(int timeout);

    int getTimeout();
    
    /**
     * Processing by TransactionTemplate must be disabled in some cases, like OutboundRouterCollection 
     * 
     * @return true, if this Transaction config should be processed by TransactionTemplate 
     */ 
    boolean isEnabled(); 

    void setEnabled(boolean enabled);
}

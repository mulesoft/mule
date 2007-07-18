/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo;

import org.mule.transaction.constraints.ConstraintFilter;

/**
 * <code>UMOTransactionConfig</code> defines transaction configuration for a
 * transactional endpoint.
 */
public interface UMOTransactionConfig
{
    /** Whether there is a transaction available or not, ignore it */
    byte ACTION_NONE = 0;

    /** Will ensure that a new transaction is created for each invocation */
    byte ACTION_ALWAYS_BEGIN = 1;

    /** Will begin a new transaction if no transaction is already present */
    byte ACTION_BEGIN_OR_JOIN = 2;

    /** There must always be a transaction present for the invocation */
    byte ACTION_ALWAYS_JOIN = 3;

    /** If there is a transaction available, then use it, otherwise continue processing */
    byte ACTION_JOIN_IF_POSSIBLE = 4;

    UMOTransactionFactory getFactory();

    void setFactory(UMOTransactionFactory factory);

    byte getAction();

    void setAction(byte action);

    boolean isTransacted();

    ConstraintFilter getConstraint();

    void setConstraint(ConstraintFilter constraint);

    void setTimeout(int timeout);

    int getTimeout();
}

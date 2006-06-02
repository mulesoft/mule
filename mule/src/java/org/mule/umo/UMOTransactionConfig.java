/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */

package org.mule.umo;

import org.mule.transaction.constraints.ConstraintFilter;

/**
 * <p>
 * <code>UMOTransactionConfig</code> defines transaction configuration for a
 * transactional endpoint.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface UMOTransactionConfig
{
    byte ACTION_NONE = 0;
    byte ACTION_ALWAYS_BEGIN = 1;
    byte ACTION_BEGIN_OR_JOIN = 2;
    byte ACTION_ALWAYS_JOIN = 3;
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

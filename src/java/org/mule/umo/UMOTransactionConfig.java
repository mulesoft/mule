/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk 
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */

package org.mule.umo;

import org.mule.transaction.constraints.ConstraintFilter;

/**
 * <p><code>UMOTransactionConfig</code> defines transaction configuration for
 * a transactional endpoint.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public interface UMOTransactionConfig
{
    public static final byte ACTION_NONE = 0;
    public static final byte ACTION_ALWAYS_BEGIN = 1;
    public static final byte ACTION_BEGIN_OR_JOIN = 2;
    public static final byte ACTION_ALWAYS_JOIN = 3;
    public static final byte ACTION_JOIN_IF_POSSIBLE = 4;

    public UMOTransactionFactory getFactory();

    public void setFactory(UMOTransactionFactory factory);

    public byte getBeginAction();

    public void setBeginAction(byte action);

    public boolean isTransacted();

    public ConstraintFilter getConstraint();

    public void setConstraint(ConstraintFilter constraint);

    public void setTimeout(int timeout);

    public int getTimeout();
}

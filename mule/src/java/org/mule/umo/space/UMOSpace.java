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
 */
package org.mule.umo.space;

import org.mule.umo.UMOTransactionFactory;
import org.mule.umo.lifecycle.Disposable;


/**
 * A space provides a "store" for shared objects.  Spaces are tread-safe and can be transactional and can also be distributed
 * allowing a shared memory space between multiple clients on a network.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface UMOSpace extends Disposable {

    public void addListener(UMOSpaceEventListener listener);

    public void removeListener(UMOSpaceEventListener listener);

    public String getName();

    public void put(Object value) throws UMOSpaceException;

    public void put(Object value, long lease) throws UMOSpaceException;

    public Object take() throws UMOSpaceException;

    public Object take(long timeout)  throws UMOSpaceException;

    public Object takeNoWait() throws UMOSpaceException;

    public int size();

    void setTransactionFactory(UMOTransactionFactory txFactory);

    UMOTransactionFactory getTransactionFactory();

    void beginTransaction() throws UMOSpaceException;

    void commitTransaction() throws UMOSpaceException;

    void rollbackTransaction() throws UMOSpaceException;

}

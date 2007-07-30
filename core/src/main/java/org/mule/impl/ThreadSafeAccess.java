/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl;

/**
 * Interface implemented by message-related objects that avoid exposing mutable data to multiple threads
 * by providing immutable copies.  This interface is optional - it is an implementation detail that is
 * tested for dynamically and used only if available.
 *
 * <p>To avoid "scribbling" where several threads change state within in a single method (typically
 * in inconsistent ways, causing subtle and intermittent errors) we use the following access policy for
 * message related objects:</p>
 *
 * <ul>
 *
 * <li>A new object is "unbound" and "mutable".</li>
 *
 * <li>An object is "bound" to the first thread that calls the object after it is created.</li>
 *
 * <li>A "mutable" object can be modified only by the thread to which it is bound.</li>
 *
 * <li>An object is "sealed" (no longer "mutable") when it is accessed by a thread other than the
 * thread to which it is "bound".  It is an error to attempt to change a "sealed" object.</li>
 *
 * </ul>
 *
 * <p>In practice this means that objects are initially mutable, but become immutable once they are
 * shared.</p>
 */
public interface ThreadSafeAccess
{

    /**
     * This method may be called before data in the object are accessed.  It should verify that the
     * access policy is followed correctly (if not, a runtime exception may be thrown).
     *
     * @param write True if the access will mutate values.
     */
    void assertAccess(boolean write);

    /**
     * This method should ONLY be used in the construction of composite ThreadSafeAccess instances.
     * For example, a ThreadSafeAccess UMOEvent contains a ThreadSafeAccess UMOMessageAdapter. During
     * the construction of the event, the message adapter may be bound to the contructing thread.
     * Calling this method releases that binding so that the event as a whole can be passed to a new
     * thread unbound.
     */
    void resetAccessControl();

    /**
     * @return A new instance of the implementing class, unbound to any thread and mutable.
     */
    ThreadSafeAccess newThreadCopy();

}

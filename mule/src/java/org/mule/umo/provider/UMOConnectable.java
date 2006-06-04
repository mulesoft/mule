/*
 * $Id$
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
package org.mule.umo.provider;

/**
 * Interface for objects that should connect to a resource.
 *
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public interface UMOConnectable {

    /**
     * Make the connection to the underlying transport. 
     * The fact that this object is connected or not should
     * have no influence on the lifecycle, especially
     * the start / stop state if applicable.
     * 
     * @throws Exception
     */
    void connect() throws Exception;

    /**
     * Disconnect the from the underlying transport
     * 
     * @throws Exception
     */
    void disconnect() throws Exception;

    /**
     * Determines if this object is connected or not
     * 
     * @return
     */
    boolean isConnected();
    
    /**
     * Returns a string identifying the underlying resource
     * @return
     */
    String getConnectionDescription();
}

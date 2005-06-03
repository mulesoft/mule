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

package org.mule.umo.lifecycle;

/**
 * <code>Disposable</code> is a lifecycle interface that gets called at the
 * dispose lifecycle stage of the implementing component as the component is
 * being destroyed.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface Disposable
{
    /**
     * A lifecycle method where implementor should fee up any resources If an
     * exception is thrown it should just be logged and processing should
     * continue. This method should not throw Runtime exceptions
     */
    void dispose();
}

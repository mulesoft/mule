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
package org.mule.umo.manager;

import java.io.Reader;
import java.util.Map;

/**
 * @author ROSS
 *         <p/>
 *         To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Generation - Code and Comments
 */
public interface UMOContainerContext
{
    public Object getComponent(Object key) throws ObjectNotFoundException;
    public void configure(Reader configuration, Map configurationProperties) throws ContainerException;
}

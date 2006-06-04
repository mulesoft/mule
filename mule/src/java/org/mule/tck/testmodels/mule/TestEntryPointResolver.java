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

package org.mule.tck.testmodels.mule;

import org.mule.model.DynamicEntryPointResolver;
import org.mule.umo.UMODescriptor;
import org.mule.umo.model.ModelException;
import org.mule.umo.model.UMOEntryPoint;

/**
 * <code>TestEntryPointResolver</code> TODO (document class)
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class TestEntryPointResolver extends DynamicEntryPointResolver
{

    /**
     * 
     */
    public TestEntryPointResolver()
    {
        super();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.spi.UMOEntryPointResolver#resolveEntryPoint(org.mule.umo.UMODescriptor)
     */
    public UMOEntryPoint resolveEntryPoint(UMODescriptor componentDescriptor) throws ModelException
    {
        return super.resolveEntryPoint(componentDescriptor);
    }

}

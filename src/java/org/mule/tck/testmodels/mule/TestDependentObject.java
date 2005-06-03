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
package org.mule.tck.testmodels.mule;

import java.util.Map;

import org.mule.config.PropertyFactory;
import org.mule.tck.testmodels.fruit.Orange;

/**
 * <code>TestDependentObject</code> is used as a mock dependency for an object
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class TestDependentObject implements PropertyFactory
{
    public Object create(Map properties) throws Exception
    {
        // make sure that both test properties are set here
        if (properties.get("test1") == null || properties.get("test2") == null) {
            throw new Exception("Both properties should be set before the factory method is called");
        }
        return new Orange();
    }
}

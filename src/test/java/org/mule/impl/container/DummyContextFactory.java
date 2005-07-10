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

package org.mule.impl.container;

import org.mule.impl.container.MuleContainerContext;
import org.mule.tck.model.AbstractContainerContextTestCase;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.umo.UMODescriptor;
import org.mule.umo.manager.UMOContainerContext;

import javax.naming.InitialContext;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import java.util.Map;
import java.util.HashMap;
import java.util.Hashtable;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class DummyContextFactory implements InitialContextFactory
{
    public Context getInitialContext(Hashtable environment) throws NamingException {
        return new DummyContext();
    }
}

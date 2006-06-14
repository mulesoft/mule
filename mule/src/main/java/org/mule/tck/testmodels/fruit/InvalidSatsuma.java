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

package org.mule.tck.testmodels.fruit;

/**
 * <code>InvalidSatsuma</code> has no discoverable methods
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class InvalidSatsuma implements Fruit
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -6328691504772842584L;

    private boolean bitten = false;

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.testmodels.fruit.Fruit#bite()
     */
    public void bite()
    {
        bitten = true;

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.testmodels.fruit.Fruit#isBitten()
     */
    public boolean isBitten()
    {
        return bitten;
    }

}

/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util;

public class ObjectUtils extends org.apache.commons.lang.ObjectUtils
{

    /**
     * Like {@link #identityToString(Object)} but without the object's full package
     * name.
     *
     * @param obj the object for which the identity description is to be generated
     * @return the object's identity description in the form of
     *         "ClassName@IdentityCode" or "null" if the argument was null.
     */
    public static String identityToShortString(Object obj)
    {
        if (obj == null)
        {
            return "null";
        }
        else
        {
            return new StringBuffer(40).append(
                    ClassUtils.getSimpleName(obj.getClass()))
                    .append('@')
                    .append(Integer.toHexString(System.identityHashCode(obj))
                    ).toString();
        }
    }

}
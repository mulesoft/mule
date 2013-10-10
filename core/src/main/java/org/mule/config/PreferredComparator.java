/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config;

import java.util.Comparator;

public class PreferredComparator implements Comparator<Preferred>
{

    public int compare(Preferred preferred1, Preferred preferred2)
    {
        if (preferred1 == null && preferred2 == null)
        {
            return 0;
        }

        if (preferred1 != null && preferred2 == null)
        {
            return 1;
        }

        if (preferred1 == null)
        {
            return -1;
        }

        return new Integer(preferred1.weight()).compareTo(preferred2.weight());
    }
}

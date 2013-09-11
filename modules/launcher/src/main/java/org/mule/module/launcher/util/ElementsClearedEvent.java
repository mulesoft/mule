/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ElementsClearedEvent extends ElementEvent
{
    private List<Object> values = new ArrayList<Object>();

    public ElementsClearedEvent(Object source, List<?> values)
    {
        super(source, OLDVALUE, NEWVALUE, ElementEvent.CLEARED, 0);
        if (values != null)
        {
            this.values.addAll(values);
        }
    }

    public List<?> getValues()
    {
        return Collections.unmodifiableList(values);
    }
}

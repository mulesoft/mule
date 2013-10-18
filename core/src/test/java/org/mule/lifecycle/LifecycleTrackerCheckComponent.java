/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.lifecycle;

import java.util.ArrayList;
import java.util.List;

public class LifecycleTrackerCheckComponent extends LifecycleTrackerComponent
{

    private final List<String> tracker = new ArrayList<String>()
    {
        public boolean add(String phase)
        {
            if (isValidTransition(phase))
            {
                return super.add(phase);
            }
            throw new IllegalStateException(String.format("Invalid phase transition: %s -> %s", this.toString(), phase));
        }

        private boolean isValidTransition(String phase)
        {
            // just check if the same phase was already invoked
            return !this.contains(phase);
        }
    };

    public List<String> getTracker()
    {
        return tracker;
    }

}



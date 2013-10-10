/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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



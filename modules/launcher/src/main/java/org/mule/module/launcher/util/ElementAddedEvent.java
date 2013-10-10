/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.launcher.util;

/**
*
*/
public class ElementAddedEvent extends ElementEvent {

    public ElementAddedEvent(Object source, Object newValue, int index) {
        super(source, null, newValue, index, ElementEvent.ADDED);
    }
}

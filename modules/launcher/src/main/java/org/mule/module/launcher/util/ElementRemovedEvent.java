/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.util;

/**
*
*/
public class ElementRemovedEvent extends ElementEvent {

    public ElementRemovedEvent(Object source, Object newValue, int index) {
        super(source, null, newValue, index, ElementEvent.REMOVED);
    }
}

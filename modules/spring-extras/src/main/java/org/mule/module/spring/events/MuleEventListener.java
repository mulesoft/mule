/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.spring.events;

import org.springframework.context.ApplicationListener;

/**
 * <code>MuleEventListener</code> is a interface that identifies an object as
 * wanting to receive Mule Events
 */

public interface MuleEventListener extends ApplicationListener
{
    // just a marker
}

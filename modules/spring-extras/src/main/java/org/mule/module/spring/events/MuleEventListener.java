/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.spring.events;

import org.springframework.context.ApplicationListener;

/**
 * <code>MuleEventListener</code> is a interface that identifies an object as
 * wanting to receive Mule Events
 *
 * <b>Deprecated from 3.6.0.</b>
 */
@Deprecated
public interface MuleEventListener extends ApplicationListener
{
    // just a marker
}

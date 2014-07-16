/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ajax;

import org.mule.api.MuleException;

import org.mortbay.cometd.AbstractBayeux;

/**
 * Set on objects that have or need access to the Bayeux CometD object
 */
public interface BayeuxAware
{
    void setBayeux(AbstractBayeux bayeux) throws MuleException;

    AbstractBayeux getBayeux();
}

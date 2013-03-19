/**
 * Mule Development Kit
 * Copyright 2010-2012 (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * This software is protected under international copyright law. All use of this software is
 * subject to MuleSoft's Master Subscription Agreement (or other master license agreement)
 * separately entered into in writing between you and MuleSoft. If such an agreement is not
 * in place, you may not use the software.
 */



package org.mule.api.callback;

import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;

public interface HttpCallback extends Startable, Stoppable, MuleContextAware {

    String getUrl();
}
/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.notifications;

import org.mule.api.component.Component;
import org.mule.api.construct.FlowConstruct;

import org.codehaus.jackson.annotate.JsonIgnore;

public interface ComponentMessageNotificationMixin
{
    @JsonIgnore
    Object getSource();

    @JsonIgnore
    Component getComponent();

    @JsonIgnore
    FlowConstruct getFlowConstruct();

}

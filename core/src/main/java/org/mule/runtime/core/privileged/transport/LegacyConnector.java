/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.transport;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.meta.NameableObject;
import org.mule.runtime.core.api.connector.Connectable;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.lifecycle.LifecycleStateEnabled;

/**
 * @deprecated Transport infrastructure is deprecated.
 */
@Deprecated
public interface LegacyConnector extends Lifecycle, NameableObject, Connectable, LifecycleStateEnabled {

  /**
   * Only use this method to use the Connector's MuleContext. Otherwise you can be used the wrong MuleContext because a Connector
   * can be defined at the domain level or de app level.
   *
   * @return MuleContext in which this connector has been created. If the Connector was defined in a Domain then it will return
   *         the MuleContext of the domain. If the Connector was defined in a Mule app then it will return the MuleContext of the
   *         Mule app.
   */
  MuleContext getMuleContext();

}

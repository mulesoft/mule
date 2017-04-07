/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.source;

import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import org.mule.extensions.jms.api.message.MessageBuilder;
import org.mule.extensions.jms.api.publish.JmsPublishParameters;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;

/**
 * Component that allows to configure JMS response.
 *
 * @since 4.0
 */
@Alias("response-builder")
@XmlHints(allowTopLevelDefinition = true)
public class JmsListenerResponseBuilder {

  @Parameter
  @Optional
  @NullSafe
  @Expression(NOT_SUPPORTED)
  private MessageBuilder messageBuilder;

  @ParameterGroup(name = "Reply Configuration")
  private JmsPublishParameters overrides;

  public MessageBuilder getMessageBuilder() {
    return messageBuilder;
  }

  public JmsPublishParameters getOverrides() {
    return overrides;
  }
}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.internal.message;

import static com.google.common.collect.ImmutableList.of;

import java.util.List;

/**
 * Declaration of the predefined names for JMSX properties,
 * according to JMS spec.
 *
 * @since 4.0
 */
public class JMSXDefinedPropertiesNames {

  public static final String JMSXUserID = "JMSXUserID";

  public static final String JMSXAppID = "JMSXAppID";

  public static final String JMSXDeliveryCount = "JMSXDeliveryCount";

  public static final String JMSXGroupID = "JMSXGroupID";

  public static final String JMSXGroupSeq = "JMSXGroupSeq";

  public static final String JMSXProducerTXID = "JMSXProducerTXID";

  public static final String JMSXConsumerTXID = "JMSXConsumerTXID";

  public static final String JMSXRcvTimestamp = "JMSXRcvTimestamp";

  public static final List<String> JMSX_NAMES = of(JMSXUserID, JMSXAppID, JMSXDeliveryCount, JMSXGroupID, JMSXGroupSeq,
                                                   JMSXProducerTXID, JMSXConsumerTXID, JMSXRcvTimestamp);

}

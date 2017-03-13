package org.mule.extensions.jms;

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */


import org.mule.extensions.jms.api.config.JmsConfig;
import org.mule.extensions.jms.api.connection.caching.CachingStrategy;
import org.mule.extensions.jms.api.connection.caching.DefaultCachingStrategy;
import org.mule.extensions.jms.api.connection.caching.NoCachingConfiguration;
import org.mule.extensions.jms.api.connection.factory.jndi.CachedJndiNameResolver;
import org.mule.extensions.jms.api.connection.factory.jndi.JndiConnectionFactory;
import org.mule.extensions.jms.api.connection.factory.jndi.JndiNameResolver;
import org.mule.extensions.jms.api.connection.factory.jndi.SimpleJndiNameResolver;
import org.mule.extensions.jms.api.destination.ConsumerType;
import org.mule.extensions.jms.api.destination.QueueConsumer;
import org.mule.extensions.jms.api.destination.TopicConsumer;
import org.mule.extensions.jms.api.exception.JmsErrors;
import org.mule.extensions.jms.api.exception.JmsExceptionHandler;
import org.mule.extensions.jms.api.operation.JmsAcknowledge;
import org.mule.extensions.jms.internal.connection.provider.GenericConnectionProvider;
import org.mule.extensions.jms.internal.connection.provider.activemq.ActiveMQConnectionProvider;
import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Export;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.OnException;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.SubTypeMapping;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.extension.api.annotation.error.ErrorTypes;

import javax.jms.ConnectionFactory;


/**
 * <code>JmsExtension</code> is a JMS 1.0.2b, 1.1 and 2.0 compliant MuleSoft Extension,
 * used to consume and produce JMS Messages.
 * The Extension supports all JMS functionality including topics and queues,
 * durable subscribers, acknowledgement modes and local transactions.
 *
 * @since 4.0
 */
@Extension(name = "JMS")
@Xml(prefix = "jms")
@Configurations({JmsConfig.class})
@ConnectionProviders({GenericConnectionProvider.class, ActiveMQConnectionProvider.class})
@Operations(JmsAcknowledge.class)
@SubTypeMapping(
    baseType = ConsumerType.class, subTypes = {QueueConsumer.class, TopicConsumer.class})
@SubTypeMapping(
    baseType = CachingStrategy.class, subTypes = {DefaultCachingStrategy.class, NoCachingConfiguration.class})
@SubTypeMapping(
    baseType = ConnectionFactory.class, subTypes = {JndiConnectionFactory.class})
@SubTypeMapping(
    baseType = JndiNameResolver.class, subTypes = {SimpleJndiNameResolver.class, CachedJndiNameResolver.class})
@ErrorTypes(JmsErrors.class)
@Export(classes = {ConnectionFactory.class})
@OnException(JmsExceptionHandler.class)
public class JmsExtension {

}

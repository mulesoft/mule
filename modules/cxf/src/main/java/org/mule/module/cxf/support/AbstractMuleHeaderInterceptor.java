/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf.support;

import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;

import static org.mule.module.cxf.MuleSoapHeaders.MULE_10_ACTOR;
import static org.mule.module.cxf.MuleSoapHeaders.MULE_HEADER;
import static org.mule.module.cxf.MuleSoapHeaders.MULE_NAMESPACE;

import static org.mule.api.config.MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY;
import static org.mule.api.config.MuleProperties.MULE_CORRELATION_ID_PROPERTY;
import static org.mule.api.config.MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY;
import static org.mule.api.config.MuleProperties.MULE_REPLY_TO_PROPERTY;

/**
 *
 */
abstract class AbstractMuleHeaderInterceptor extends AbstractPhaseInterceptor<Message>
{
    protected final static String MULE_NS_URI = MULE_10_ACTOR;
    protected final static String MULE_NS_PREFIX = MULE_NAMESPACE;
    protected final static String MULE_XMLNS = "xmlns:" + MULE_NS_PREFIX;
    protected final static String QUALIFIED_MULE_HEADER = MULE_NS_PREFIX + ":" + MULE_HEADER;

    protected static final QName MULE_HEADER_Q = new QName(MULE_NS_URI, MULE_HEADER);

    protected static final Set<QName> UNDERSTOOD_HEADERS = new HashSet<QName>();
    static
    {
        UNDERSTOOD_HEADERS.add(MULE_HEADER_Q);
    }

    protected static final Set<String> SUPPORTED_HEADERS = new HashSet<String>();

    static
    {
        SUPPORTED_HEADERS.add(MULE_CORRELATION_GROUP_SIZE_PROPERTY);
        SUPPORTED_HEADERS.add(MULE_CORRELATION_ID_PROPERTY);
        SUPPORTED_HEADERS.add(MULE_CORRELATION_SEQUENCE_PROPERTY);
        SUPPORTED_HEADERS.add(MULE_REPLY_TO_PROPERTY);
    }

    public AbstractMuleHeaderInterceptor(String p)
    {
        super(p);
    }

}

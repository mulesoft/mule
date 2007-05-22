/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.i18n;

/**
 * Internationalised Migration messages. These are the messages used when generating the migration
 * report.
 */
public class MigrationMessages extends MessageFactory
{
    private static final String BUNDLE_PATH = getBundlePath("migration");

    public static Message modelAttributeNotSupported()
    {
        return createMessage(BUNDLE_PATH, 1);
    }

    public static Message recoverableModeAttributeNotSupported()
    {
        return createMessage(BUNDLE_PATH, 2);
    }

    public static Message clientModeAttributeNotSupported()
    {
        return createMessage(BUNDLE_PATH, 3);
    }

    public static Message embeddedAttributeNotSupported()
    {
        return createMessage(BUNDLE_PATH, 4);
    }

    public static Message blankServerUrl()
    {
        return createMessage(BUNDLE_PATH, 5);
    }

    public static Message serverUrlNotSupported()
    {
        return createMessage(BUNDLE_PATH, 6);
    }

    public static Message enableMessageEventsNotSupported()
    {
        return createMessage(BUNDLE_PATH, 7);
    }

    public static Message refAttributeNotSupported()
    {
        return createMessage(BUNDLE_PATH, 8);
    }

    public static Message cannotEmbedSpringXml()
    {
        return createMessage(BUNDLE_PATH, 9);
    }

    public static Message jndiContainerContextNotSupported()
    {
        return createMessage(BUNDLE_PATH, 10);
    }

    public static Message endpointIdentifiersNotSupported()
    {
        return createMessage(BUNDLE_PATH, 11);
    }

    public static Message interceptorsNotSupported()
    {
        return createMessage(BUNDLE_PATH, 12);
    }

    public static Message containerManagedAttributeNotSupported()
    {
        return createMessage(BUNDLE_PATH, 13);
    }

    public static Message containerAttributeNotSupported()
    {
        return createMessage(BUNDLE_PATH, 14);
    }

    public static Message inboundEndpointAttributeNotSupported()
    {
        return createMessage(BUNDLE_PATH, 15);
    }

    public static Message inboundTransformerAttributeNotSupported()
    {
        return createMessage(BUNDLE_PATH, 16);
    }

    public static Message outboundEndpointAttributeNotSupported()
    {
        return createMessage(BUNDLE_PATH, 17);
    }

    public static Message outboundTransformerAttributeNotSupported()
    {
        return createMessage(BUNDLE_PATH, 18);
    }

    public static Message responseTransformerAttributeNotSupported()
    {
        return createMessage(BUNDLE_PATH, 19);
    }

    public static Message poolingProfileElementNotSupportedInMuleConfiguration()
    {
        return createMessage(BUNDLE_PATH, 20, "pooling-profile");
    }

    public static Message queueProfileElementNotSupportedInMuleConfiguration()
    {
        return createMessage(BUNDLE_PATH, 20, "queue-profile");
    }

    public static Message persistenceElementNotSupported()
    {
        return createMessage(BUNDLE_PATH, 21);
    }
}


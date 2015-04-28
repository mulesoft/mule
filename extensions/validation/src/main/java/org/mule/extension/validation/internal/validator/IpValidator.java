/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.internal.validator;

import static org.mule.extension.validation.internal.ImmutableValidationResult.ok;
import org.mule.api.MuleEvent;
import org.mule.config.i18n.Message;
import org.mule.extension.validation.api.ValidationResult;
import org.mule.extension.validation.internal.ValidationContext;

import com.google.common.net.InetAddresses;

/**
 * A {@link AbstractValidator} which checks that a given
 * {@link #ip} is valid. It supports both IPV4 and IPV6.
 *
 * In the case of IPV6, both full and collapsed addresses
 * are supported, but addresses containing ports are not
 *
 * @since 3.7.0
 */
public class IpValidator extends AbstractValidator
{

    private final String ip;

    public IpValidator(String ip, ValidationContext validationContext)
    {
        super(validationContext);
        this.ip = ip;
    }

    @Override
    public ValidationResult validate(MuleEvent event)
    {
        return InetAddresses.isInetAddress(ip)
               ? ok()
               : fail();
    }

    @Override
    protected Message getDefaultErrorMessage()
    {
        return getMessages().invalidIp(ip);
    }
}

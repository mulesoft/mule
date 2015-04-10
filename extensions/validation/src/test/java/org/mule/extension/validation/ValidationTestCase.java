/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation;

import org.mule.extension.validation.internal.ValidationExtension;
import org.mule.extension.validation.internal.ValidationMessages;
import org.mule.tck.junit4.ExtensionsFunctionalTestCase;

abstract class ValidationTestCase extends ExtensionsFunctionalTestCase
{

    static final String VALID_URL = "http://localhost:8080";
    static final String INVALID_URL = "here";

    static final String VALID_ISBN13 = "978-1-56619-909-4 ";
    static final String INVALID_ISBN13 = "88";
    static final String VALID_ISBN10 = "1-56619-909-3";
    static final String INVALID_ISBN10 = "88";

    static final String VALID_EMAIL = "mariano.gonzalez@mulesoft.com";
    static final String INVALID_EMAIL = "@mulesoft.com";

    /**
     * number obtained from
     * <a href="http://www.paypalobjects.com/en_US/vhelp/paypalmanager_help/credit_card_numbers.htm">
     * this link</a>
     */
    static final String VALID_CREDIT_CARD_NUMBER = "5555555555554444";
    static final String INVALID_CREDIT_CARD_NUMBER = "5555444433332222";

    static final RuntimeException CUSTOM_VALIDATOR_EXCEPTION = new RuntimeException();

    protected ValidationMessages messages;

    @Override
    protected Class<?>[] getAnnotatedExtensionClasses()
    {
        return new Class<?>[] {ValidationExtension.class};
    }

    @Override
    protected void doSetUp() throws Exception
    {
        messages = new ValidationMessages();
    }
}

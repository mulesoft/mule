/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.internal.validator;

import static org.apache.commons.validator.routines.UrlValidator.ALLOW_2_SLASHES;
import static org.apache.commons.validator.routines.UrlValidator.ALLOW_ALL_SCHEMES;
import static org.apache.commons.validator.routines.UrlValidator.ALLOW_LOCAL_URLS;
import static org.apache.commons.validator.routines.UrlValidator.NO_FRAGMENTS;
import static org.mule.extension.validation.internal.ImmutableValidationResult.ok;
import org.mule.api.MuleEvent;
import org.mule.config.i18n.Message;
import org.mule.extension.validation.api.ValidationResult;
import org.mule.extension.validation.internal.ValidationContext;

/**
 * An {@link AbstractValidator} which verifies
 * that a given {@link #url} is valid.
 *
 * @since 3.7.0
 */
public class UrlValidator extends AbstractValidator
{

    /**
     * the url to be tested
     */
    private final String url;

    /**
     * if {@code true}, double slashes are allowed
     */
    private final boolean allowTwoSlashes;

    /**
     * if {@code true}, all schemes are allowed
     */
    private final boolean allowAllSchemes;

    /**
     * if {@code true}, local urls are allowed
     */
    private final boolean allowLocalUrls;

    /**
     * if {@code true}, fragments are not allowed
     */
    private final boolean noFragments;

    public UrlValidator(String url,
                        boolean allowTwoSlashes,
                        boolean allowAllSchemes,
                        boolean allowLocalUrls,
                        boolean noFragments,
                        ValidationContext validationContext)
    {
        super(validationContext);
        this.url = url;
        this.allowTwoSlashes = allowTwoSlashes;
        this.allowAllSchemes = allowAllSchemes;
        this.allowLocalUrls = allowLocalUrls;
        this.noFragments = noFragments;
    }

    @Override
    public ValidationResult validate(MuleEvent event)
    {
        long options = 0;

        if (allowAllSchemes)
        {
            options |= ALLOW_ALL_SCHEMES;
        }
        if (allowTwoSlashes)
        {
            options |= ALLOW_2_SLASHES;
        }
        if (allowLocalUrls)
        {
            options |= ALLOW_LOCAL_URLS;
        }
        if (noFragments)
        {
            options |= NO_FRAGMENTS;
        }

        org.apache.commons.validator.routines.UrlValidator validator = new org.apache.commons.validator.routines.UrlValidator(options);

        return validator.isValid(url)
               ? ok()
               : fail();
    }

    @Override
    protected Message getDefaultErrorMessage()
    {
        return getMessages().invalidUrl(url);
    }
}

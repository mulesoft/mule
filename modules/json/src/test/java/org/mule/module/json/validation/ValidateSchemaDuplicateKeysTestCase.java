/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json.validation;

import static java.util.Arrays.asList;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.rules.ExpectedException.none;
import static org.mule.module.json.validation.JsonSchemaTestUtils.SCHEMA_FSTAB_JSON;
import static org.mule.module.json.validation.JsonSchemaTestUtils.getFstabWithDuplicateKeys;
import static org.mule.module.json.validation.ValidateJsonSchemaMessageProcessor.ALLOW_DUPLICATE_KEYS_SYSTEM_PROPERTY;
import static org.mule.module.json.validation.ValidateSchemaFunctionalTestCase.DEREFERENCING;
import static org.mule.module.json.validation.ValidateSchemaFunctionalTestCase.SCHEMA_LOCATION;

import org.mule.tck.junit4.rule.SystemProperty;

import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.internal.matchers.ThrowableMessageMatcher;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ValidateSchemaDuplicateKeysTestCase extends AbstractValidateSchemaFunctionalTestCase
{

    @Rule
    public SystemProperty dereferencing = new SystemProperty(DEREFERENCING, "false");

    @Rule
    public SystemProperty schemaLocation = new SystemProperty(SCHEMA_LOCATION, SCHEMA_FSTAB_JSON);

    @Rule
    public SystemProperty duplicateKeys;

    @Rule
    public ExpectedException expectedException;

    public ValidateSchemaDuplicateKeysTestCase(String allowDuplicateKeys, ExpectedException expectedException)
    {
        this.expectedException = expectedException;
        duplicateKeys = new SystemProperty(ALLOW_DUPLICATE_KEYS_SYSTEM_PROPERTY, allowDuplicateKeys);
    }

    @Parameters
    public static Collection<Object[]> data()
    {
        return asList(new Object[][] {
                {"true", none()},
                {"false", getExpectedExceptionForDuplicateKeys()}
        });
    }

    @Override
    protected String getConfigFile()
    {
        return "validate-schema-duplicate-keys-config.xml";
    }

    @Test
    public void duplicateKeys() throws Exception
    {
        runFlow(VALIDATE_FLOW, getFstabWithDuplicateKeys());
    }

    private static ExpectedException getExpectedExceptionForDuplicateKeys()
    {
        ExpectedException expectedException = none();
        expectedException.expectCause(new ThrowableMessageMatcher<>(containsString("Duplicate field")));
        return expectedException;
    }

}

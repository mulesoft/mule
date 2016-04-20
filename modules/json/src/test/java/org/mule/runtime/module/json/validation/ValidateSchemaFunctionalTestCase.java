/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.json.validation;

import static com.github.fge.jackson.JsonLoader.fromString;
import static org.junit.runners.Parameterized.Parameter;
import static org.mule.runtime.module.json.validation.JsonSchemaDereferencing.CANONICAL;
import static org.mule.runtime.module.json.validation.JsonSchemaDereferencing.INLINE;
import static org.mule.runtime.module.json.validation.JsonSchemaTestUtils.SCHEMA_FSTAB_DRAFTV3;
import static org.mule.runtime.module.json.validation.JsonSchemaTestUtils.SCHEMA_FSTAB_INLINE;
import static org.mule.runtime.module.json.validation.JsonSchemaTestUtils.SCHEMA_FSTAB_JSON;
import static org.mule.runtime.module.json.validation.JsonSchemaTestUtils.SCHEMA_FSTAB_REFERRING;
import static org.mule.runtime.module.json.validation.JsonSchemaTestUtils.getBadFstab;
import static org.mule.runtime.module.json.validation.JsonSchemaTestUtils.getBadFstab2;
import static org.mule.runtime.module.json.validation.JsonSchemaTestUtils.getBadFstab2AsJsonData;
import static org.mule.runtime.module.json.validation.JsonSchemaTestUtils.getBadFstabAsJsonData;
import static org.mule.runtime.module.json.validation.JsonSchemaTestUtils.getGoodFstab;
import static org.mule.runtime.module.json.validation.JsonSchemaTestUtils.getGoodFstabAsJsonData;
import static org.mule.runtime.module.json.validation.JsonSchemaTestUtils.getGoodFstabInline;
import static org.mule.runtime.module.json.validation.JsonSchemaTestUtils.getGoodFstabInlineAsJsonData;
import static org.mule.runtime.module.json.validation.JsonSchemaTestUtils.toStream;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ValidateSchemaFunctionalTestCase extends AbstractValidateSchemaFunctionalTestCase
{

    private static final String SCHEMA_LOCATION = "namespaceLocation";
    private static final String DEREFERENCING = "dereferencing";

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Object[]> data() throws Exception
    {
        return Arrays.asList(new Object[][] {
                {"SimpleV4Schema as String", SCHEMA_FSTAB_JSON, CANONICAL, getGoodFstab(), getBadFstab(), getBadFstab2()},
                {"SimpleV4Schema as bytes", SCHEMA_FSTAB_JSON, CANONICAL, getGoodFstab().getBytes(), getBadFstab().getBytes(), getBadFstab2().getBytes()},
                {"SimpleV4Schema as JsonNode", SCHEMA_FSTAB_JSON, CANONICAL, fromString(getGoodFstab()), fromString(getBadFstab()), fromString(getBadFstab2())},
                {"SimpleV4Schema as JsonData", SCHEMA_FSTAB_JSON, CANONICAL, getGoodFstabAsJsonData(), getBadFstabAsJsonData(), getBadFstabAsJsonData()},
                {"SimpleV4Schema as Stream", SCHEMA_FSTAB_JSON, CANONICAL, toStream(getGoodFstab()), toStream(getBadFstab()), toStream(getBadFstab2())},

                {"Inline schema as String", SCHEMA_FSTAB_INLINE, INLINE, getGoodFstabInline(), getBadFstab(), getBadFstab2()},
                {"Inline schema as bytes", SCHEMA_FSTAB_INLINE, INLINE, getGoodFstabInline().getBytes(), getBadFstab().getBytes(), getBadFstab2().getBytes()},
                {"Inline schema as JsonNode", SCHEMA_FSTAB_INLINE, INLINE, fromString(getGoodFstabInline()), fromString(getBadFstab()), fromString(getBadFstab2())},
                {"Inline schema as JsonData", SCHEMA_FSTAB_INLINE, INLINE, getGoodFstabInlineAsJsonData(), getBadFstabAsJsonData(), getBadFstab2AsJsonData()},
                {"Inline schema as Stream", SCHEMA_FSTAB_INLINE, INLINE, toStream(getGoodFstabInline()), toStream(getBadFstab()), toStream(getBadFstab2())},

                {"Draft3 as String", SCHEMA_FSTAB_DRAFTV3, CANONICAL, getGoodFstab(), getBadFstab(), getBadFstab2()},
                {"Draft3 as bytes", SCHEMA_FSTAB_DRAFTV3, CANONICAL, getGoodFstab().getBytes(), getBadFstab().getBytes(), getBadFstab2().getBytes()},
                {"Draft3 as JsonNode", SCHEMA_FSTAB_DRAFTV3, CANONICAL, fromString(getGoodFstab()), fromString(getBadFstab()), fromString(getBadFstab2())},
                {"Draft3 as JsonData", SCHEMA_FSTAB_DRAFTV3, CANONICAL, getGoodFstabAsJsonData(), getBadFstabAsJsonData(), getBadFstabAsJsonData()},
                {"Draft3 as Stream", SCHEMA_FSTAB_DRAFTV3, CANONICAL, toStream(getGoodFstab()), toStream(getBadFstab()), toStream(getBadFstab2())},

                {"ReferringV4Schema as String", SCHEMA_FSTAB_REFERRING, CANONICAL, getGoodFstab(), getBadFstab(), getBadFstab2()},
                {"ReferringV4Schema as bytes", SCHEMA_FSTAB_REFERRING, CANONICAL, getGoodFstab().getBytes(), getBadFstab().getBytes(), getBadFstab2().getBytes()},
                {"ReferringV4Schema as JsonNode", SCHEMA_FSTAB_REFERRING, CANONICAL, fromString(getGoodFstab()), fromString(getBadFstab()), fromString(getBadFstab2())},
                {"ReferringV4Schema as JsonData", SCHEMA_FSTAB_REFERRING, CANONICAL, getGoodFstabAsJsonData(), getBadFstabAsJsonData(), getBadFstabAsJsonData()},
                {"ReferringV4Schema as Stream", SCHEMA_FSTAB_REFERRING, CANONICAL, toStream(getGoodFstab()), toStream(getBadFstab()), toStream(getBadFstab2())},
        });
    }

    @Parameter(0)
    public String description;

    @Parameter(1)
    public String schemaLocation;

    @Parameter(2)
    public JsonSchemaDereferencing dereferencing;

    @Parameter(3)
    public Object goodJson;

    @Parameter(4)
    public Object badJson;

    @Parameter(5)
    public Object badJson2;


    @Override
    protected String getConfigFile()
    {
        return "validate-schema-config.xml";
    }

    @Override
    protected void doSetUpBeforeMuleContextCreation() throws Exception
    {
        System.setProperty(SCHEMA_LOCATION, schemaLocation);
        System.setProperty(DEREFERENCING, dereferencing.name());
    }

    @Override
    protected void doTearDown() throws Exception
    {
        System.clearProperty(SCHEMA_LOCATION);
        System.clearProperty(DEREFERENCING);
    }

    @Test
    public void good() throws Exception
    {
        flowRunner(VALIDATE_FLOW).withPayload(goodJson).run();
    }

    @Test(expected = JsonSchemaValidationException.class)
    public void bad() throws Throwable
    {
        runAndExpectFailure(badJson);
    }

    @Test(expected = JsonSchemaValidationException.class)
    public void bad2() throws Throwable
    {
        runAndExpectFailure(badJson2);
    }
}

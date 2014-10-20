/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json.validation;

import static com.github.fge.jackson.JsonLoader.fromString;
import static org.junit.runners.Parameterized.Parameter;
import static org.junit.runners.Parameterized.Parameters;
import static org.mule.module.json.validation.JsonSchemaTestUtils.FAKE_SCHEMA_URI;
import static org.mule.module.json.validation.JsonSchemaTestUtils.SCHEMA_FSTAB_INLINE;
import static org.mule.module.json.validation.JsonSchemaTestUtils.SCHEMA_FSTAB_JSON;
import static org.mule.module.json.validation.JsonSchemaTestUtils.getBadFstab;
import static org.mule.module.json.validation.JsonSchemaTestUtils.getBadFstab2;
import static org.mule.module.json.validation.JsonSchemaTestUtils.getBadFstab2AsJsonData;
import static org.mule.module.json.validation.JsonSchemaTestUtils.getBadFstabAsJsonData;
import static org.mule.module.json.validation.JsonSchemaTestUtils.getGoodFstab;
import static org.mule.module.json.validation.JsonSchemaTestUtils.getGoodFstabAsJsonData;
import static org.mule.module.json.validation.JsonSchemaTestUtils.getGoodFstabInline;
import static org.mule.module.json.validation.JsonSchemaTestUtils.getGoodFstabInlineAsJsonData;
import static org.mule.module.json.validation.JsonSchemaTestUtils.toStream;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.TransformerException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transformer.types.DataTypeFactory;

import java.io.InputStream;
import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class JsonSchemaValidatorTestCase extends AbstractMuleContextTestCase
{

    @Parameters(name = "{0}")
    public static Iterable<Object[]> data() throws Exception
    {
        return Arrays.asList(new Object[][] {
                {"SimpleV4Schema as String", getFstabValidator(), getGoodFstab(), getBadFstab(), getBadFstab2()},
                {"SimpleV4Schema as bytes", getFstabValidator(), getGoodFstab().getBytes(), getBadFstab().getBytes(), getBadFstab2().getBytes()},
                {"SimpleV4Schema as JsonNode", getFstabValidator(), fromString(getGoodFstab()), fromString(getBadFstab()), fromString(getBadFstab2())},
                {"SimpleV4Schema as JsonData", getFstabValidator(), getGoodFstabAsJsonData(), getBadFstabAsJsonData(), getBadFstabAsJsonData()},
                {"SimpleV4Schema as Stream", getFstabValidator(), toStream(getGoodFstab()), toStream(getBadFstab()), toStream(getBadFstab2())},

                {"Inline schema as String", getInlineFstabValidator(), getGoodFstabInline(), getBadFstab(), getBadFstab2()},
                {"Inline schema as bytes", getInlineFstabValidator(), getGoodFstabInline().getBytes(), getBadFstab().getBytes(), getBadFstab2().getBytes()},
                {"Inline schema as JsonNode", getInlineFstabValidator(), fromString(getGoodFstabInline()), fromString(getBadFstab()), fromString(getBadFstab2())},
                {"Inline schema as JsonData", getInlineFstabValidator(), getGoodFstabInlineAsJsonData(), getBadFstabAsJsonData(), getBadFstab2AsJsonData()},
                {"Inline schema as Stream", getInlineFstabValidator(), toStream(getGoodFstabInline()), toStream(getBadFstab()), toStream(getBadFstab2())},

                {"Draft3 as String", getDraft3Validator(), getGoodFstab(), getBadFstab(), getBadFstab2()},
                {"Draft3 as bytes", getDraft3Validator(), getGoodFstab().getBytes(), getBadFstab().getBytes(), getBadFstab2().getBytes()},
                {"Draft3 as JsonNode", getDraft3Validator(), fromString(getGoodFstab()), fromString(getBadFstab()), fromString(getBadFstab2())},
                {"Draft3 as JsonData", getDraft3Validator(), getGoodFstabAsJsonData(), getBadFstabAsJsonData(), getBadFstab2AsJsonData()},
                {"Draft3 as Stream", getDraft3Validator(), toStream(getGoodFstab()), toStream(getBadFstab()), toStream(getBadFstab2())},


                {"Schema with redirects as String", getFstabValidatorWithRedirects(), getGoodFstab(), getBadFstab(), getBadFstab2()},
                {"Schema with redirects as bytes", getFstabValidatorWithRedirects(), getGoodFstab().getBytes(), getBadFstab().getBytes(), getBadFstab2().getBytes()},
                {"Schema with redirects as JsonNode", getFstabValidatorWithRedirects(), fromString(getGoodFstab()), fromString(getBadFstab()), fromString(getBadFstab2())},
                {"Schema with redirects as JsonData", getFstabValidatorWithRedirects(), getGoodFstabAsJsonData(), getBadFstabAsJsonData(), getBadFstab2AsJsonData()},
                {"Schema with redirects as Stream", getFstabValidatorWithRedirects(), toStream(getGoodFstab()), toStream(getBadFstab()), toStream(getBadFstab2())},
        });
    }

    @Parameter(0)
    public String description;

    @Parameter(1)
    public JsonSchemaValidator validator;

    @Parameter(2)
    public Object goodJson;

    @Parameter(3)
    public Object badJson;

    @Parameter(4)
    public Object badJson2;

    @Test
    public void good() throws Exception
    {
        validator.validate(getTestEvent(goodJson));
    }

    @Test(expected = JsonSchemaValidationException.class)
    public void bad() throws Exception
    {
        validator.validate(getTestEvent(badJson));
    }

    @Test(expected = JsonSchemaValidationException.class)
    public void bad2() throws Exception
    {
        validator.validate(getTestEvent(badJson2));
    }

    @Test
    public void goodThroughTransformer() throws Exception
    {
        muleContext.getRegistry().registerTransformer(new AppleToJson(goodJson));
        try
        {
            validator.validate(getTestEvent(new Apple()));
        }
        catch (JsonSchemaValidationException e)
        {
            if (goodJson instanceof InputStream)
            {
                // do nothing, streams are not supported through transformation
            }
            else
            {
                throw e;
            }
        }
    }

    private class AppleToJson extends AbstractMessageTransformer implements DiscoverableTransformer
    {

        private Object value;

        private AppleToJson(Object value)
        {
            this.value = value;
            registerSourceType(DataTypeFactory.create(Apple.class));
            if (value instanceof InputStream)
            {
                setReturnDataType(DataTypeFactory.create(InputStream.class));
            }
            else
            {
                setReturnDataType(DataTypeFactory.create(value.getClass()));
            }
        }

        @Override
        public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException
        {
            return value;
        }


        @Override
        public int getPriorityWeighting()
        {
            return 100;
        }

        @Override
        public void setPriorityWeighting(int weighting)
        {

        }
    }

    private static JsonSchemaValidator getFstabValidator()
    {
        return getSimpleValidator(SCHEMA_FSTAB_JSON);
    }

    private static JsonSchemaValidator getFstabValidatorWithRedirects()
    {
        return JsonSchemaValidator.builder()
                .setSchemaLocation(FAKE_SCHEMA_URI)
                .addSchemaRedirect(FAKE_SCHEMA_URI, SCHEMA_FSTAB_JSON)
                .build();
    }

    private static JsonSchemaValidator getDraft3Validator()
    {
        return getSimpleValidator(JsonSchemaTestUtils.SCHEMA_FSTAB_DRAFTV3);
    }

    private static JsonSchemaValidator getInlineFstabValidator()
    {
        return JsonSchemaValidator.builder()
                .setSchemaLocation(SCHEMA_FSTAB_INLINE)
                .setDereferencing(JsonSchemaDereferencing.INLINE)
                .build();
    }


    private static JsonSchemaValidator getSimpleValidator(String schemaLocation)
    {
        return JsonSchemaValidator.builder()
                .setSchemaLocation(schemaLocation)
                .build();
    }
}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json.validation;

import org.mule.module.json.JsonData;
import org.mule.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

abstract class JsonSchemaTestUtils
{

    protected static final String SCHEMA_FSTAB_GOOD_JSON = "schema/fstab-good.json";
    protected static final String SCHEMA_FSTAB_BAD_JSON = "schema/fstab-bad.json";
    protected static final String SCHEMA_FSTAB_BAD2_JSON = "schema/fstab-bad2.json";
    protected static final String SCHEMA_FSTAB_GOOD_INLINE_JSON = "schema/fstab-good-inline.json";
    protected static final String SCHEMA_FSTAB_DRAFTV3 = "/schema/fstab-draftv3.json";
    protected static final String SCHEMA_FSTAB_INLINE = "/schema/fstab-inline.json";
    protected static final String SCHEMA_FSTAB_REFERRING = "/schema/fstab-referring.json";
    protected static final String SCHEMA_FSTAB_JSON = "/schema/fstab.json";
    protected static final String FAKE_SCHEMA_URI = "http://mule.org/schemas/fstab.json";

    protected static String getGoodFstab() throws Exception
    {
        return doGetResource(SCHEMA_FSTAB_GOOD_JSON);
    }

    protected static JsonData getGoodFstabAsJsonData() throws Exception
    {
        return new JsonData(getGoodFstab());
    }

    protected static String getBadFstab() throws Exception
    {
        return doGetResource(SCHEMA_FSTAB_BAD_JSON);
    }

    protected static JsonData getBadFstabAsJsonData() throws Exception
    {
        return new JsonData(getBadFstab());
    }

    protected static String getBadFstab2() throws Exception
    {
        return doGetResource(SCHEMA_FSTAB_BAD2_JSON);
    }

    protected static JsonData getBadFstab2AsJsonData() throws Exception
    {
        return new JsonData(getBadFstab2());
    }

    protected static String getGoodFstabInline() throws Exception
    {
        return doGetResource(SCHEMA_FSTAB_GOOD_INLINE_JSON);
    }

    protected static JsonData getGoodFstabInlineAsJsonData() throws Exception
    {
        return new JsonData(getGoodFstabInline());
    }

    protected static String doGetResource(String path) throws Exception
    {
        return IOUtils.getResourceAsString(path, JsonSchemaValidatorTestCase.class);
    }

    protected static InputStream toStream(String content)
    {
        return new ByteArrayInputStream(content.getBytes());
    }
}

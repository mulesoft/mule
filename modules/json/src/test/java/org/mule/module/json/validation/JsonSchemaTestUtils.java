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

    static final String SCHEMA_FSTAB_GOOD_JSON = "schema/fstab-good.json";
    static final String SCHEMA_FSTAB_DUPLICATE_KEYS = "schema/fstab-duplicate-keys.json";
    static final String SCHEMA_FSTAB_BAD_JSON = "schema/fstab-bad.json";
    static final String SCHEMA_FSTAB_BAD2_JSON = "schema/fstab-bad2.json";
    static final String SCHEMA_FSTAB_GOOD_INLINE_JSON = "schema/fstab-good-inline.json";
    static final String SCHEMA_FSTAB_DRAFTV3 = "/schema/fstab-draftv3.json";
    static final String SCHEMA_FSTAB_INLINE = "/schema/fstab-inline.json";
    static final String SCHEMA_FSTAB_REFERRING = "/schema/fstab-referring.json";
    static final String SCHEMA_FSTAB_JSON = "/schema/fstab.json";
    static final String FAKE_SCHEMA_URI = "http://mule.org/schemas/fstab.json";

    static String getGoodFstab() throws Exception
    {
        return doGetResource(SCHEMA_FSTAB_GOOD_JSON);
    }

    static String getFstabWithDuplicateKeys () throws Exception
    {
        return doGetResource(SCHEMA_FSTAB_DUPLICATE_KEYS);
    }

    static JsonData getGoodFstabAsJsonData() throws Exception
    {
        return new JsonData(getGoodFstab());
    }

    static String getBadFstab() throws Exception
    {
        return doGetResource(SCHEMA_FSTAB_BAD_JSON);
    }

    static JsonData getBadFstabAsJsonData() throws Exception
    {
        return new JsonData(getBadFstab());
    }

    static String getBadFstab2() throws Exception
    {
        return doGetResource(SCHEMA_FSTAB_BAD2_JSON);
    }

    static JsonData getBadFstab2AsJsonData() throws Exception
    {
        return new JsonData(getBadFstab2());
    }

    static String getGoodFstabInline() throws Exception
    {
        return doGetResource(SCHEMA_FSTAB_GOOD_INLINE_JSON);
    }

    static JsonData getGoodFstabInlineAsJsonData() throws Exception
    {
        return new JsonData(getGoodFstabInline());
    }

    static String doGetResource(String path) throws Exception
    {
        return IOUtils.getResourceAsString(path, JsonSchemaValidatorTestCase.class);
    }

    static InputStream toStream(String content)
    {
        return new ByteArrayInputStream(content.getBytes());
    }
}

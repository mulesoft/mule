/*
 * $Header: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//httpclient/src/java/org/apache/commons/httpclient/MultiThreadedHttpConnectionManager.java,v 1.47 2004/12/21 11:27:55 olegk Exp $
 * $Revision: 564906 $
 * $Date: 2007-08-11 14:27:18 +0200 (Sat, 11 Aug 2007) $
 *
 * ====================================================================
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.mule.module.db.integration.template;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.module.db.internal.domain.query.QueryTemplate;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class CdataParameterizedQueryTemplateTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "integration/template/cdata-parameterized-query-template-config.xml";
    }

    @Test
    public void parsesParameterizedQuery() throws Exception
    {
        QueryTemplate queryTemplate = muleContext.getRegistry().lookupObject("parameterizedQuery");
        assertThat(queryTemplate.getSqlText(), equalTo("select * from PLANET"));
    }
}

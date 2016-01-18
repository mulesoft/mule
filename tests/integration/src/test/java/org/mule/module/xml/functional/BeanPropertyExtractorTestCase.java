/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.functional;

import java.util.Properties;

public class BeanPropertyExtractorTestCase extends AbstractXmlPropertyExtractorTestCase
{

    @Override
    protected Properties getStartUpProperties()
    {
        Properties p = new Properties();
        p.setProperty("selector.expression", "payload.childBean.value");
        return p;
    }

    @Override
    protected Object getMatchMessage()
    {
        // Model a simple bean graph. Path is: childBean.value
        return new TestRootBean(new TestValueBean("match"));
    }

    @Override
    protected Object getErrorMessage()
    {
        return new TestRootBean(null);
    }

    public class TestRootBean
    {
        private TestValueBean childBean;

        public TestRootBean(TestValueBean childBean)
        {
            this.childBean = childBean;
        }

        public TestValueBean getChildBean()
        {
            return childBean;
        }

        public void setChildBean(TestValueBean childBean)
        {
            this.childBean = childBean;
        }
    }

    public class TestValueBean
    {
        private String value;

        public TestValueBean(String value)
        {
            this.value = value;
        }

        public String getValue()
        {
            return value;
        }

        public void setValue(String value)
        {
            this.value = value;
        }
    }

}

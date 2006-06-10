/* 
* $Id$
* ------------------------------------------------------------------------------------------------------
* 
* Copyright (c) SymphonySoft Limited. All rights reserved.
* http://www.symphonysoft.com
* 
* The software in this package is published under the terms of the BSD
* style license a copy of which has been included with this distribution in
* the LICENSE.txt file. 
*
*/
package org.mule.config.builders;

import org.apache.commons.digester.SetPropertiesRule;
import org.xml.sax.Attributes;

/**
 * this rule Allows for
 * template parameters to be parse on the configuration file attributes in the form of
 * ${param-name}. These will get resolved against properties set in the
 * mule-properites element
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MuleSetPropertiesRule extends SetPropertiesRule
 {
    protected PlaceholderProcessor processor;

    public MuleSetPropertiesRule() {
        processor = new PlaceholderProcessor();
    }

    public MuleSetPropertiesRule(PlaceholderProcessor processor) {
        this.processor = processor;
    }

    public MuleSetPropertiesRule(String[] strings, String[] strings1) {
        super(strings, strings1);
        processor = new PlaceholderProcessor();
    }

    public MuleSetPropertiesRule(String[] strings, String[] strings1, PlaceholderProcessor processor) {
        super(strings, strings1);
        this.processor = processor;
    }

    public void begin(String s1, String s2, Attributes attributes) throws Exception {
        attributes = processor.processAttributes(attributes, s2);
        super.begin(attributes);
    }
}

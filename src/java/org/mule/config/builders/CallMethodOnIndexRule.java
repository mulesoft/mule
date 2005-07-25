/* 
* $Header$
* $Revision$
* $Date$
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

import org.apache.commons.digester.CallMethodRule;

/**
 * todo document
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class CallMethodOnIndexRule extends CallMethodRule 
{
    int index = 0;

    public CallMethodOnIndexRule(String s, int i, int index) {
        super(s, i);
        this.index = index;
    }

    public CallMethodOnIndexRule(String s, int index) {
        super(s);
        this.index = index;
    }

    public void end(String string, String string1) throws Exception
    {
        Object o = digester.peek(index);
        digester.push(o);
        super.end(string, string1);
        o = digester.pop();
    }

}

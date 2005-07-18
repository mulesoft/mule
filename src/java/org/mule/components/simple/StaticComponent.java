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
package org.mule.components.simple;

import org.mule.umo.UMOEventContext;
import org.mule.umo.lifecycle.Callable;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.RecoverableException;
import org.mule.util.Utility;

import java.io.IOException;

/**
 * A component that will return a static data object as a result.  This is useful
 * for testing with expected results.  The data returned can be read from a file or
 * set as a property on this component.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class StaticComponent implements Callable, Initialisable {

    private Object data;
    private String dataFile;

    public void initialise() throws InitialisationException, RecoverableException {
        if(dataFile!=null)
        {
            try {
                data = Utility.loadResourceAsString(dataFile, getClass());
            } catch (IOException e) {
                throw new InitialisationException(e, this);
            }
        }
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getDataFile() {
        return dataFile;
    }

    public void setDataFile(String dataFile) {
        this.dataFile = dataFile;
    }

    public Object onCall(UMOEventContext eventContext) throws Exception {
        return data;
    }
}

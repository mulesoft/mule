//COPYRIGHT
package org.mule.util;

import java.io.IOException;
import java.io.Reader;

//AUTHOR

public class ChainedReader extends Reader
{
    private Reader first;
    private Reader second;
    private boolean firstRead = false;

    public ChainedReader(Reader first, Reader second) {
        this.first = first;
        this.second = second;
    }

    public void close() throws IOException {
        first.close();
        second.close();
    }

    public int read(char cbuf[], int off, int len) throws IOException
    {
        if(!firstRead) {
            int i = first.read(cbuf, off, len);
            if(i < len) {
                firstRead = true;
                int x = second.read(cbuf, i, len - i);
                return x+i;
            } else {
                return i;
            }
        } else {
            return second.read(cbuf, off, len);
        }
    }

    public int read() throws IOException {
        if(!firstRead) {
            int i = first.read();
            if(i ==-1) {
                firstRead = true;
                return second.read();
            } else {
                return i;
            }
        } else {
            return second.read();
        }
    }

    public int read(char cbuf[]) throws IOException {
        if(!firstRead) {
            int i = first.read(cbuf);
            if(i < cbuf.length) {
                firstRead = true;
                int x = second.read(cbuf, i, cbuf.length - i);
                return x+i;
            } else {
                return i;
            }
        } else {
            return second.read(cbuf);
        }
    }
}

package org.mule.module.launcher.descriptor;

import java.io.File;
import java.io.IOException;

/**
 *
 */
public interface DescriptorParser
{

    ApplicationDescriptor parse(File descriptor) throws IOException;
}

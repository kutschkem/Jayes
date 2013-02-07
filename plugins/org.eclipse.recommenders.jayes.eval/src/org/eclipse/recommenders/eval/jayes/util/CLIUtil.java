/*******************************************************************************
 * Copyright (c) 2013 Michael Kutschke.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Michael Kutschke - initial API and implementation
 ******************************************************************************/
package org.eclipse.recommenders.eval.jayes.util;

import java.util.Arrays;

public class CLIUtil {

    public static final String OSGi_CMD_ARGS = "application.args";
    public static final String CONFIG_PARAM = "config";
    public static final String OUTPUTDIR = "evaluation.outputDirectory";
    public static final String MODULES = "evaluation.modules";
    public static final String EVALUATION_DATA_FILE = "evalData.json";

    public static String findArg(String[] args, String string) {
        int index = Arrays.asList(args).indexOf("-" + string) + 1;
        if (index == 0)
            return null;
        return args[index];
    }

}

/**
 * Copyright (c) 2011 Michael Kutschke.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Michael Kutschke - initial API and implementation.
 */
package org.eclipse.recommenders.jayes.io.util;

public class XMLUtil {

    public static String surround(String content, String surroundingTag) {
        return String.format("<%1$s>%2$s</%1$s>", surroundingTag, content);
    }

}

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
        return String.format("<%1$s>%2$s\n</%1$s>", surroundingTag, addTab("\n" + content));
    }

    /**
     * this method expects the attributes in pairwise name, value form e.g.
     * </br> attributes = [ "id", "12345", "size", "15" ]
     * 
     * @param content
     * @param surroundingTag
     * @param attributes
     * @return
     */
    public static String surround(String content, String surroundingTag, String... attributes) {
        StringBuilder attributeBuilder = new StringBuilder();

        for (int i = 0; i < attributes.length; i += 2) {
            attributeBuilder.append(attributes[i]);
            attributeBuilder.append("=\"");
            attributeBuilder.append(attributes[i + 1]);
            attributeBuilder.append("\" ");
        }

        return String.format("<%1$s %2$s>%3$s\n</%1$s>", surroundingTag, attributeBuilder.toString(), addTab("\n"
                + content));
    }

    /**
     * adds a tab to every line
     * 
     * @param text
     * @return
     */
    public static String addTab(String text) {
        return text.replaceAll("\n", "\n\t");
    }

    public static String emptyTag(String tagname, String... attributes) {
        StringBuilder attributeBuilder = new StringBuilder();

        for (int i = 0; i < attributes.length; i += 2) {
            attributeBuilder.append(attributes[i]);
            attributeBuilder.append("=\"");
            attributeBuilder.append(attributes[i + 1]);
            attributeBuilder.append("\" ");
        }

        return String.format("<%1$s %2$s/>", tagname, attributeBuilder.toString());
    }

}

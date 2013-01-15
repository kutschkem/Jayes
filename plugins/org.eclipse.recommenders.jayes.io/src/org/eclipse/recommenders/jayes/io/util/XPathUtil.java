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

import java.util.Iterator;

import org.w3c.dom.Node;
import org.w3c.dom.xpath.XPathEvaluator;
import org.w3c.dom.xpath.XPathResult;

public class XPathUtil {

    private static XPathResult result = null;

    public static Iterator<Node> evalXPath(XPathEvaluator eval, String xpath, Node context) {

        result = ((XPathResult) eval.evaluate(xpath, context, null, (short) 0, result));

        return new Iterator<Node>() {

            Node cache = null;

            @Override
            public boolean hasNext() {
                if (cache == null) {
                    cache = result.iterateNext();
                }
                return cache != null;
            }

            @Override
            public Node next() {
                if (cache == null) {
                    return result.iterateNext();
                }
                Node n = cache;
                cache = null;
                return n;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();

            }

        };
    }

}

package org.eclipse.recommenders.jayes.io.util;

import java.util.Iterator;

import org.w3c.dom.Node;
import org.w3c.dom.xpath.XPathEvaluator;
import org.w3c.dom.xpath.XPathResult;

public class XPathUtil {

    public static Iterator<Node> evalXPath(XPathEvaluator eval, String xpath, Node context) {

        final XPathResult result = ((XPathResult) eval.evaluate(xpath, context, null, (short) 0, null));

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

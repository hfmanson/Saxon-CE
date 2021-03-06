package client.net.sf.saxon.ce.expr.instruct;

import client.net.sf.saxon.ce.Controller;
import client.net.sf.saxon.ce.event.ReceiverOptions;
import client.net.sf.saxon.ce.event.SequenceReceiver;
import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.lib.StandardURIChecker;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.pattern.NodeKindTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.type.TypeHierarchy;
import client.net.sf.saxon.ce.value.Whitespace;

import java.util.ArrayList;
import java.util.Iterator;

/**
* A namespace constructor instruction. (xsl:namespace in XSLT 2.0, or namespace{}{} in XQuery 1.1)
*/

public class NamespaceConstructor extends SimpleNodeConstructor {

    private Expression name;

    /**
     * Create an xsl:namespace instruction for dynamic construction of namespace nodes
     * @param name the expression to evaluate the name of the node (that is, the prefix)
     */

    public NamespaceConstructor(Expression name) {
        this.name = name;
        adoptChildExpression(name);
    }

    /**
    * Set the name of this instruction for diagnostic and tracing purposes
    */

    public int getInstructionNameCode() {
        return StandardNames.XSL_NAMESPACE;
    }

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        name = visitor.simplify(name);
        return super.simplify(visitor);
    }

    public ItemType getItemType(TypeHierarchy th) {
        return NodeKindTest.NAMESPACE;
    }

    public int getCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    protected void promoteInst(PromotionOffer offer) throws XPathException {
        if (select != null) {
            select = doPromotion(select, offer);
        }
        name = doPromotion(name, offer);
        super.promoteInst(offer);
    }

    public void localTypeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        name = visitor.typeCheck(name, contextItemType);
    }

    public Iterator<Expression> iterateSubExpressions() {
        ArrayList list = new ArrayList(6);
        if (select != null) {
            list.add(select);
        }
        list.add(name);
        return list.iterator();
    }


    /**
     * Replace one subexpression by a replacement subexpression
     * @param original the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        boolean found = false;
        if (select == original) {
            select = replacement;
            found = true;
        }
        if (name == original) {
            name = replacement;
            found = true;
        }
                return found;
    }


    private String evaluatePrefix(XPathContext context) throws XPathException {
        String prefix = Whitespace.trim(name.evaluateAsString(context));
        if (!(prefix.length() == 0 || NameChecker.isValidNCName(prefix))) {
            dynamicError("Namespace prefix is invalid: " + prefix, "XTDE0920", context);
        }

        if (prefix.equals("xmlns")) {
            dynamicError("Namespace prefix 'xmlns' is not allowed", "XTDE0920", context);
        }
        return prefix;
    }

    public int evaluateNameCode(XPathContext context) throws XPathException {
        String prefix = evaluatePrefix(context);
        return context.getNamePool().allocate("", "", prefix);
    }

    public void processValue(CharSequence value, XPathContext context) throws XPathException {
        Controller controller = context.getController();
        String prefix = evaluatePrefix(context);
        String uri = value.toString();
        checkPrefixAndUri(prefix, uri, context);

        SequenceReceiver out = context.getReceiver();
        out.namespace(new NamespaceBinding(prefix, uri), ReceiverOptions.REJECT_DUPLICATES);
    }


    /**
     * Evaluate as an expression. We rely on the fact that when these instructions
     * are generated by XQuery, there will always be a valueExpression to evaluate
     * the content
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        NodeInfo node = (NodeInfo)super.evaluateItem(context);
        String prefix = node.getLocalPart();
        String uri = node.getStringValue();
        checkPrefixAndUri(prefix, uri, context);
        return node;
    }

    private void checkPrefixAndUri(String prefix, String uri, XPathContext context) throws XPathException {
        if (prefix.equals("xml") != uri.equals(NamespaceConstant.XML)) {
            dynamicError("Namespace prefix 'xml' and namespace uri " + NamespaceConstant.XML +
                    " must only be used together", "XTDE0925", context);
        }

        if (uri.length() == 0) {
            dynamicError("Namespace URI is an empty string", "XTDE0930", context);
        }

        if (uri.equals(NamespaceConstant.XMLNS)) {
            dynamicError("A namespace node cannot have the reserved namespace " +
                    NamespaceConstant.XMLNS, "XTDE0935", context);
        }

        if (!StandardURIChecker.getInstance().isValidURI(uri)) {
            dynamicError("The string value of the constructed namespace node must be a valid URI", "XTDE0905", context);
        }
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.

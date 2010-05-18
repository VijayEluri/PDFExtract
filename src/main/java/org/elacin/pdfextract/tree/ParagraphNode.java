/*
 * Copyright 2010 Øyvind Berg (elacin@gmail.com)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.elacin.pdfextract.tree;

import org.elacin.pdfextract.Loggers;

import java.util.Comparator;

import static org.elacin.pdfextract.util.MathUtils.isWithinVariance;


/**
 * Created by IntelliJ IDEA.
 * User: elacin
 * Date: Apr 8, 2010
 * Time: 8:56:45 AM
 * To change this template use File | Settings | File Templates.
 */
public class ParagraphNode extends AbstractParentNode<LineNode, PageNode> {
    // --------------------------- CONSTRUCTORS ---------------------------

    public ParagraphNode() {
    }

    // -------------------------- PUBLIC METHODS --------------------------

    @Override
    public boolean addWord(final WordNode node) {
        if (getChildren().isEmpty()) {
            if (Loggers.getCreateTreeLog().isDebugEnabled()) {
                Loggers.getCreateTreeLog().debug(toString() + ": Adding a new LineNode for node " + node);
            }
            addChild(new LineNode(node));
            return true;
        }

        /* try the lines already added and see if this fits at the end of one of them */
        for (LineNode line : getChildren()) {
            if (line.addWord(node)) {
                return true;
            }
        }

        if (isNewLineInThisParagraph(node)) {
            if (Loggers.getCreateTreeLog().isDebugEnabled()) {
                Loggers.getCreateTreeLog().debug(toString() + ": Node " + node + " is considered to be a new line in this paragraph");
            }
            addChild(new LineNode(node));
            return true;
        }


        return false;
    }

    @Override
    public void combineChildren() {

        /* combine all contained line nodes which are clearly on the same line.
         *  using array to avoid concurrent modification of iterator */
        LineNode[] lineNodes = getChildren().toArray(new LineNode[getChildren().size()]);

        for (int i = 0; i < lineNodes.length; i++) {
            if (lineNodes[i] == null) {
                continue;
            }

            LineNode firstLine = lineNodes[i];

            for (int j = 0; j < lineNodes.length; j++) {
                if (i == j) continue;

                if (lineNodes[j] == null) {
                    continue;
                }
                LineNode secondLine = lineNodes[j];

                if (firstLine.isOnSameLine(secondLine)) {
                    if (Loggers.getCreateTreeLog().isDebugEnabled()) {
                        Loggers.getCreateTreeLog().debug("combining lines " + firstLine + " and " + secondLine);
                    }
                    firstLine.combineWith(secondLine);

                    /* remove secondLine */
                    lineNodes[j] = null;
                    getChildren().remove(secondLine);
                }
            }
        }

        /* call one level further down the tree */
        for (LineNode lineNode : getChildren()) {
            lineNode.combineChildren();
        }
    }

    /**
     * Adds all lines from otherParagraph to this paragraph. No checks here!
     *
     * @param otherParagraph
     */
    public void combineWith(final ParagraphNode otherParagraph) {
        /* add the lines which we could not combine with the existing lines in the paragraph */
        for (LineNode lineNode : otherParagraph.getChildren()) {
            addChild(lineNode);
        }
    }

    /**
     * Returns a Comparator which compares coordinates within a page
     *
     * @return
     */
    @Override
    public Comparator<LineNode> getChildComparator() {
        return new StandardNodeComparator();
    }

    public boolean isNewLineInThisParagraph(final AbstractNode node) {

        LineNode lastLineNode = getChildren().get(getChildren().size() - 1);

        /* if the last line still has no text, accept this */
        if (lastLineNode.getChildren().isEmpty()) {
            return true;
        }

        /* Use the size of the preceding and new text to determine if the two lines are
             logically part of the same paragraph. Use the smaller of the two values.
         */
        double ySize = Math.min(lastLineNode.getCurrentStyle().ySize, node.getStyle().ySize);
        //        double ySize = lastLineNode.getCurrentStyle().ySize;

        if (isWithinVariance(getPosition().getY() + getPosition().getHeight(), node.getPosition().getY(), ySize)) {

            double widthOfSpace = lastLineNode.getCurrentStyle().widthOfSpace;

            /* if the new node STARTS at more or less the same X position, accept it */
            if (isWithinVariance(getPosition().getX(), node.getPosition().getX(), widthOfSpace)) {
                return true;
            }

            /* if the new node ENDS at more or less the same X position, also accept it */
            if (isWithinVariance(getPosition().getX() + getPosition().getWidth(), node.getPosition().getX() + node.getPosition().getWidth(), widthOfSpace)) {
                return true;
            }

            /* also accept it if its X value IS CONTAINED between current position's lower X and (higher X + widthOfSpace)*/
            if (getPosition().getX() < node.getPosition().getX() &&
                    getPosition().getX() + getPosition().getWidth() + widthOfSpace > node.getPosition().getX()) {
                return true;
            }
        }
        return false;
    }

}

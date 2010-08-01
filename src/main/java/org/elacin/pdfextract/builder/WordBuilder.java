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

package org.elacin.pdfextract.builder;

import org.apache.log4j.Logger;
import org.apache.pdfbox.util.TextPosition;
import org.apache.pdfbox.util.TextPositionComparator;
import org.elacin.pdfextract.Loggers;
import org.elacin.pdfextract.pdfbox.ETextPosition;
import org.elacin.pdfextract.text.Style;
import org.elacin.pdfextract.tree.DocumentNode;
import org.elacin.pdfextract.tree.PageNode;
import org.elacin.pdfextract.tree.WordNode;
import org.elacin.pdfextract.util.MathUtils;
import org.elacin.pdfextract.util.Point;
import org.elacin.pdfextract.util.Rectangle;
import org.elacin.pdfextract.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.elacin.pdfextract.util.MathUtils.round;

/**
 * Created by IntelliJ IDEA.
 * User: elacin
 * Date: May 12, 2010
 * Time: 3:34:09 AM
 * <p/>
 * This class provides a way to convert incoming TextPositions (as created by PDFBox) into
 * WordNodes, as used by this application. The difference between the two classes are
 * technical, but also semantic, in that they are defined to be at most a whole word (the
 * normal case: Word fragments will only occur when a word is split over two lines, or
 * if the word is formatted with two different styles) instead of arbitrary length.
 * <p/>
 * This makes it easier to reason about the information we have, and also reconstructs
 * some notion of word and character spacing, which will be an important property
 * for feature recognition.
 * <p/>
 * By using fillPage() all this will be done, and the words will be added to the
 * the provided document tree.
 */
public class WordBuilder {
    // ------------------------------ FIELDS ------------------------------

    private static final Logger LOG = Loggers.getWordBuilderLog();

    // -------------------------- PUBLIC METHODS --------------------------

    /**
     * This method will convert the text into WordNodes, which will be added to the
     * provided DocumentNode under the correct page
     * <p/>
     * To do this, the text is split on whitespaces, character and word distances are
     * approximated, and words are created based on those
     *
     * @param root          Document to which add words
     * @param pageNum       Page number
     * @param textToBeAdded text which is to be added
     */
    public void fillPage(final DocumentNode root, final int pageNum, final List<ETextPosition> textToBeAdded) {
        long t0 = System.currentTimeMillis();

        /* iterate through all incoming TextPositions, and process them
            in a line by line fashion. We do this to be able to calculate
            char and word distances for each line
         */

        List<TextPosition> line = new ArrayList<TextPosition>();

        Collections.sort(textToBeAdded, new TextPositionComparator());

        float lastY = Float.MAX_VALUE;
        float lastEndY = Float.MIN_VALUE;
        for (ETextPosition textPosition : textToBeAdded) {
            if (LOG.isTraceEnabled()) {
                LOG.trace(StringUtils.getTextPositionString(textPosition));
            }

            /* If this not the first text on a line and also not on the same Y coordinate
                as the existing, complete this line */

            /**
             * Decide whether or not this textPosition is part of the current
             *  line we are building or not
             */
            boolean endLine = false;

            if (lastY == Float.MAX_VALUE) {
                /* if this is the first text in a line */
                lastY = textPosition.getY();
                lastEndY = lastY + textPosition.getHeightDir();
            } else if (lastY != textPosition.getYDirAdj()) {
                //            } else if (!isOnSameLine(lastY, lastEndY, textPosition.getYDirAdj(), textPosition.getYDirAdj() + textPosition.getHeightDir())) {
                /* end the current line if this element is not considered to be part of it */
                endLine = true;
            }

            if (endLine) {
                processLine(root, pageNum, line);
                line.clear();
                lastY = Float.MAX_VALUE;
            }

            line.add(textPosition);
            lastY = Math.min(textPosition.getYDirAdj(), lastY);
            lastEndY = Math.max(lastY + textPosition.getHeightDir(), lastEndY);
        }

        if (!line.isEmpty()) {
            processLine(root, pageNum, line);
            line.clear();
        }

        /* if the page contained no text, create an empty page node and return */
        boolean foundAddedPage = false;
        for (PageNode pageNode : root.getChildren()) {
            if (pageNode.getPageNumber() == pageNum) {
                foundAddedPage = true;
            }
        }
        if (!foundAddedPage) {
            root.addChild(new PageNode(pageNum));
            return;
        }

        LOG.debug("WordBuilder.fillPage took " + (System.currentTimeMillis() - t0) + " ms");
    }

    /**
     * This method will process one line worth of TextPositions , and split and/or
     * combine them as to output words. The words are added directly to the tree
     *
     * @param root
     * @param pageNum
     * @param line
     */
    void processLine(final DocumentNode root, final int pageNum, final List<TextPosition> line) {
        /* first convert into text elements */
        final List<Text> lineTexts = getTextsFromTextPositions(root.getStyles(), line);

        /* then calculate spacing */
        setCharSpacingForTexts(lineTexts);

        /* this will be used to keep all the state while combining text fragments into words */
        WordState currentState = new WordState();

        /* create WordNodes and add them to the tree */
        for (Text newText : lineTexts) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("in : " + newText);
            }

            /* if this is the first text element going into a word */
            if (currentState.len == 0) {
                currentState.x = newText.x;
                currentState.y = newText.y;
                currentState.charSpacing = newText.charSpacing;
            } else {
                /* if not, check if this new text means we should finish the current word */
                if (currentState.isTooFarAway(newText) || currentState.isDifferentStyle(newText.style)) {
                    root.addWord(currentState.createWord(pageNum));
                    currentState.x = newText.x;
                }
            }

            currentState.currentStyle = newText.style;
            currentState.maxHeight = Math.max(currentState.maxHeight, newText.height);

            /* copy text from the Text object, and adjust width */
            for (int textPositionIdx = 0; textPositionIdx < newText.content.length(); textPositionIdx++) {
                currentState.chars[currentState.len] = newText.content.charAt(textPositionIdx);
                currentState.len++;
            }
            currentState.width = newText.x + newText.width - currentState.x;
        }

        /* no words can span lines, so yield whatever is read */
        if (currentState.len != 0) {
            root.addWord(currentState.createWord(pageNum));
        }
    }

    /**
     * This creates
     *
     * @param sf
     * @param textPositions
     * @return
     */
    static List<Text> getTextsFromTextPositions(final DocumentStyles sf, final List<TextPosition> textPositions) {
        List<Text> ret = new ArrayList<Text>(textPositions.size() * 2);

        Point lastWordBoundary = new Point(0, 0);
        StringBuilder contents = new StringBuilder();

        Collections.sort(textPositions, new TextPositionComparator());

        float width = 0.0f;
        boolean firstInLine = true;
        for (TextPosition textPosition : textPositions) {
            float x = textPosition.getXDirAdj();
            float y = textPosition.getYDirAdj();
            final Style style = sf.getStyleForTextPosition(textPosition);

            for (int j = 0; j < textPosition.getCharacter().length(); j++) {
                /* if we found a space */
                if (Character.isSpaceChar(textPosition.getCharacter().charAt(j)) || isTextPositionTooHigh(textPosition)) {
                    if (contents.length() != 0) {
                        /* else just output a new text */

                        final float distance;
                        if (firstInLine) {
                            distance = Float.MIN_VALUE;
                            firstInLine = false;
                        } else {
                            distance = x - lastWordBoundary.getX();
                        }

                        ret.add(new Text(contents.toString(), style, x, y, width, textPosition.getHeightDir(), distance));
                        contents.setLength(0);
                        x += width;
                        width = 0.0F;
                        lastWordBoundary.setPosition(x, y);
                    }

                    x += textPosition.getIndividualWidths()[j];
                } else {
                    /* include this character */
                    width += textPosition.getIndividualWidths()[j];
                    contents.append(textPosition.getCharacter().charAt(j));
                }


                /* if this is the last char */
                if (j == textPosition.getCharacter().length() - 1 && contents.length() != 0) {
                    final float distance;
                    if (firstInLine) {
                        distance = Float.MIN_VALUE;
                        firstInLine = false;
                    } else {
                        distance = x - lastWordBoundary.getX();
                    }

                    /* Some times textPosition.getIndividualWidths will contain zero, so work around that here */
                    if (width == 0.0F) {
                        width = textPosition.getWidthDirAdj();
                    }

                    ret.add(new Text(contents.toString(), style, x, y, width, textPosition.getHeightDir(), distance));
                    contents.setLength(0);
                    x += width;
                    width = 0.0F;
                    lastWordBoundary.setPosition(x, y);
                }
            }
        }

        return ret;
    }

    /**
     * Some times TextPositions will be far, far higher than the font size would allow. this is normally a faulty PDF or a
     * bug in PDFBox. since they destroy my algorithms ill just drop them
     *
     * @param textPosition
     * @return
     */
    private static boolean isTextPositionTooHigh(final TextPosition textPosition) {
        return textPosition.getHeightDir() > (float) (textPosition.getFontSize() * textPosition.getYScale() * 1.2);
    }

    /**
     * @param texts
     */
    void setCharSpacingForTexts(final List<Text> texts) {
        if (texts.isEmpty()) return;

        /* Start by making a list of all distances, and an average*/
        //        List<Integer> distances = new ArrayList<Integer>(texts.size() - 1);
        int[] distances = new int[texts.size() - 1];

        int distanceCount = 0;
        int fontSizeSum = 0;
        for (int i = 0; i < texts.size(); i++) {
            Text text = texts.get(i);
            /* skip the first word fragment, and only include this distance if it is not too big */
            if (i != 0 && text.distanceToPreceeding < text.style.xSize * 6) {
                distances[distanceCount] = Math.max(0, text.distanceToPreceeding);
                distanceCount++;
            }
            fontSizeSum += text.style.xSize;
        }

        /* spit out some debug information */
        if (LOG.isDebugEnabled()) {
            StringBuilder textWithDistances = new StringBuilder();
            StringBuilder textOnly = new StringBuilder();

            for (int i = 0; i < texts.size(); i++) {
                Text text = texts.get(i);
                if (i != 0) {
                    textWithDistances.append("> ").append(text.distanceToPreceeding).append(">");
                }
                textWithDistances.append(text.content);
                textOnly.append(text.content);
            }
            LOG.debug("spacing: -----------------");
            LOG.debug("spacing: content: " + textWithDistances);
            LOG.debug("spacing: content: " + textOnly);
            LOG.debug("spacing: unsorted: " + Arrays.toString(distances));
        }

        final double fontSizeAverage = (double) (fontSizeSum / texts.size()) / 100.0;
        int charSpacing = calculateCharspacingForDistances(distances, distanceCount, fontSizeAverage);

        /* and set the values in all texts */
        for (Text text : texts) {
            text.charSpacing = charSpacing;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("spacing: sorted: " + Arrays.toString(distances));
            LOG.debug("spacing: charSpacing=" + charSpacing);

            StringBuilder out = new StringBuilder();

            for (int i = 0; i < texts.size(); i++) {
                Text text = texts.get(i);
                if (i != 0 && text.distanceToPreceeding > charSpacing) {
                    out.append(" ");
                }
                out.append(text.content);
            }
            LOG.debug("spacing: output: " + out);
        }
    }

    /**
     * This is the algorithm to find out the most probable charSpacing given a list of integer distances
     * <p/>
     * <p/>
     * There are two special cases:
     * - only one text fragment, in which case we set a spacing of 0
     * - all distances are almost the same, in which case we assume it is one continuous word, and not a list of single text fragments
     * <p/>
     * The general algorithm consists of these steps:
     *
     * @param distances
     * @param distanceCount
     * @param averageFontSize
     * @return
     */
    static int calculateCharspacingForDistances(final int[] distances, final int distanceCount, final double averageFontSize) {
        /* calculate the average distance - it will be used below */
        double sum = 0.0;
        for (int i = 0; i < distanceCount; i++) {
            sum += distances[i];
        }
        double averageDistance = sum / (double) distanceCount;
        LOG.debug("averageFontSize = " + averageFontSize + ", averageDistance = " + averageDistance);

        /* this algorithm depends on that the distances be sorted */
        Arrays.sort(distances);


        double charSpacing = Double.MIN_VALUE;
        if (distanceCount == 0) {
            charSpacing = 0.0;
        } else if (MathUtils.isWithinPercent(distances[0], distances[distanceCount - 1], 10)) {
            charSpacing = distances[distanceCount - 1];
            LOG.debug("spacing: all distances equal, setting as character space");
        } else {
            /* iterate backwards - do this because char spacing seems to vary a lot more than does word spacing */
            double biggestScore = Double.MIN_VALUE;
            double lastDistance = (double) (distances[distanceCount - 1] + 1);

            for (int i = distanceCount - 1; i >= 0; i--) {
                double distance = (double) distances[i];

                /* this is just an optimization - dont consider a certain distance if we already did */
                if (distance == charSpacing || distance == lastDistance) {
                    continue;
                }


                if (distance < Math.max(averageFontSize * 3.0, averageDistance)) {
                    /* the essential scoring here is based on */
                    double score = ((lastDistance - distance) / (Math.max(1, distance)) + distance * 0.5);

                    if (distance > 0.0) {
                        /* weight the score to give some priority to spacings around the same size as the given average font size.

                        * This works by measuring the distance from distance to the given font size as the logarithm of distance with
                        *   font size as the base.
                        * One is subtracted from this logarithm to re-base the number around 0, and the absolute value is calculated.
                        *
                        * For say distances of 32 or 8 with a font size 16, this number will in both cases be 0.25.
                        *   The penalty given to the score for that would then be score*(1 + (0.25 * 0.4))
                        * */
                        //                        score *= (Math.abs(StrictMath.log(distance) / Math.max(1.0, StrictMath.log(averageFontSize)) - 1.0) * 0.4 + 1.0);

                        final double distanceLog = Math.abs(StrictMath.log(distance) / Math.max(1.0, StrictMath.log(averageDistance))) - 1.0;
                        score *= (distanceLog * 0.1) + 1;

                        /* and give some priority to bigger distances */
                        score = score * (double) i / (double) distanceCount;
                    } else {
                        /* for zero or negative distances, divide the score by two */
                        score *= 0.5;
                    }

                    if (score > biggestScore) {
                        biggestScore = score;
                        charSpacing = distance;
                        LOG.debug("spacing: " + charSpacing + " is now the most probable. score: " + score);
                    } else {
                        LOG.debug("spacing: " + distance + " got score " + score);
                    }
                }
                lastDistance = distance;
            }
        }

        final double expectedMinimum = Math.max(3, averageFontSize / 2);
        if (charSpacing < expectedMinimum) {
            LOG.debug("spacing: got suspiciously low charSpacing " + charSpacing + " setting to " + expectedMinimum);
            charSpacing = expectedMinimum;
        }

        /* return after converting to int and correcting for rounding */
        @SuppressWarnings({"NumericCastThatLosesPrecision"}) int ret = (int) charSpacing;
        ret += 1;
        return ret;
    }

    // -------------------------- INNER CLASSES --------------------------

    private static class Text {
        final int x, y, width, height, distanceToPreceeding;
        int charSpacing;
        final String content;
        final Style style;

        Text(final String content, final Style style, final float x, final float y, final float width, final float height, final float distanceToPreceeding) {
            this.distanceToPreceeding = round(distanceToPreceeding);
            this.height = round(height);
            this.width = round(width);
            this.x = round(x);
            this.y = round(y);
            this.style = style;
            this.content = content;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("Text");
            sb.append("{d=").append(distanceToPreceeding);
            sb.append(", x=").append(x);
            sb.append(", endX=").append(x + width);
            sb.append(", y=").append(y);
            sb.append(", width=").append(width);
            sb.append(", height=").append(height);
            sb.append(", text='").append(content).append('\'');
            sb.append(", style=").append(style);
            sb.append(", charSpacing=").append(charSpacing);
            sb.append('}');
            return sb.toString();
        }
    }

    /**
     * This class is used while combining Text objects into Words, as a simple way of
     * grouping all state together.
     */
    private static class WordState {
        private final char[] chars = new char[512];
        private int len;
        private int maxHeight;
        private int width;
        private int x;
        private int y;
        private Style currentStyle;
        public int charSpacing;

        public WordNode createWord(final int pageNum) {
            String wordText = new String(chars, 0, len);
            //TODO
            final WordNode word = new WordNode(new Rectangle(x, y - maxHeight, width, maxHeight), pageNum, currentStyle, wordText, charSpacing);

            if (LOG.isDebugEnabled()) {
                LOG.debug("out: " + word);
            }

            /* then reset state for next word */
            len = 0;
            x += width;
            maxHeight = 0;
            width = 0;

            return word;
        }

        public boolean isTooFarAway(final Text text) {
            if (text.distanceToPreceeding < 0) return false;

            if (text.distanceToPreceeding > text.charSpacing) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(this + ": " + text + " is too far away");
                }
                return true;
            }

            return false;
        }

        public boolean isDifferentStyle(final Style newStyle) {
            return !currentStyle.equals(newStyle);
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("WordState");
            sb.append("{len=").append(len);
            sb.append(", maxHeight=").append(maxHeight);
            sb.append(", width=").append(width);
            sb.append(", x=").append(x);
            sb.append(", y=").append(y);
            sb.append(", currentStyle=").append(currentStyle);
            sb.append('}');
            return sb.toString();
        }
    }
}

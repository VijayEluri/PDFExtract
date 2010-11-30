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

package org.elacin.pdfextract.util;

import org.apache.pdfbox.util.TextPosition;
import org.elacin.pdfextract.StyledText;
import org.elacin.pdfextract.logical.Formulas;
import org.elacin.pdfextract.physical.content.HasPosition;
import org.elacin.pdfextract.physical.content.PhysicalContent;
import org.elacin.pdfextract.style.Style;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Created by IntelliJ IDEA. User: elacin Date: May 19, 2010 Time: 3:46:09 AM To change this
 * template use File | Settings | File Templates.
 */
public class TextUtils {
// -------------------------- PUBLIC STATIC METHODS --------------------------

@NotNull
public static Rectangle findBounds(@NotNull final Collection<? extends HasPosition> contents) {
	final Rectangle newPos;

	if (contents.isEmpty()) {
		//TODO: handle empty regions in a better way
		newPos = new Rectangle(0.1f, 0.1f, 0.1f, 0.1f);
	} else {
		/* calculate bounds for this region */
		float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
		float maxX = Float.MIN_VALUE, maxY = Float.MIN_VALUE;

		for (HasPosition content : contents) {
			minX = Math.min(minX, content.getPos().getX());
			minY = Math.min(minY, content.getPos().getY());
			maxX = Math.max(maxX, content.getPos().getEndX());
			maxY = Math.max(maxY, content.getPos().getEndY());
		}
		newPos = new Rectangle(minX, minY, maxX - minX, maxY - minY);
	}
	return newPos;
}

@NotNull
public static Style findDominatingStyle(@NotNull final Collection<? extends HasPosition> contents) {
	if (Formulas.textSeemsToBeFormula(contents)) {
		return Style.FORMULA;
	}

	if (!listContainsStyle(contents)){
		return Style.NO_STYLE;
	}

	boolean textFound = false;
	Map<Style, Integer> letterCountPerStyle = new HashMap<Style, Integer>(10);
	for (HasPosition content : contents) {
		if (!(content instanceof StyledText)){
			continue;
		}
		StyledText text = (StyledText) content;

		final Style style = text.getStyle();
		if (!letterCountPerStyle.containsKey(style)) {
			letterCountPerStyle.put(style, 0);
		}
		final int numChars = text.getText().length();
		letterCountPerStyle.put(style, letterCountPerStyle.get(style) + numChars);
		textFound = true;
	}

	assert textFound;

	int highestNumChars = -1;
	Style style = null;
	for (Map.Entry<Style, Integer> entry : letterCountPerStyle.entrySet()) {
		if (entry.getValue() > highestNumChars) {
			style = entry.getKey();
			highestNumChars = entry.getValue();
		}
	}
	return style;
}

@NotNull
public static String getTextPositionString(@NotNull final TextPosition position) {
	StringBuilder sb = new StringBuilder("pos{");
	sb.append("c=\"").append(position.getCharacter()).append("\"");
	sb.append(", XDirAdj=").append(position.getXDirAdj());
	sb.append(", YDirAdj=").append(position.getYDirAdj());
	sb.append(", endY=").append(position.getYDirAdj() + position.getHeightDir());
	sb.append(", endX=").append(position.getXDirAdj() + position.getWidthDirAdj());

	sb.append(", HeightDir=").append(position.getHeightDir());
	sb.append(", WidthDirAdj=").append(position.getWidthDirAdj());

	sb.append(", WidthOfSpace=").append(position.getWidthOfSpace());
	sb.append(", FontSize=").append(position.getFontSize());
	sb.append(", getIndividualWidths=").append(Arrays.toString(position.getIndividualWidths()));
	sb.append(", font=").append(position.getFont().getBaseFont());

	sb.append("}");
	return sb.toString();
}

public static boolean listContainsStyle(@NotNull final Collection<? extends HasPosition> list) {
	for (HasPosition content : list) {
		if (content instanceof StyledText){
			return true;
		}
	}
	return false;
}
}

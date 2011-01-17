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

import org.elacin.pdfextract.style.Style;
import org.elacin.pdfextract.style.TextUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

/**
 * Created by IntelliJ IDEA. User: elacin Date: Apr 8, 2010 Time: 8:29:43 AM To change this
 * template
 * use File | Settings | File Templates.
 */
public class LineNode extends AbstractParentNode<WordNode, ParagraphNode> {
// --------------------------- CONSTRUCTORS ---------------------------

public LineNode(@NotNull final WordNode child) {
    super(child);
}

public LineNode() {
    super();
}

// ------------------------ OVERRIDING METHODS ------------------------

/**
 * Returns a Comparator which compares only X coordinates
 */
@NotNull
@Override
public Comparator getChildComparator() {
    return new Comparator<WordNode>() {
        public int compare(@NotNull final WordNode o1, @NotNull final WordNode o2) {
            if (o1.getPos().getX() < o2.getPos().getX()) {
                return -1;
            } else if (o1.getPos().getX() > o2.getPos().getX()) {
                return 1;
            }

            return 0;
        }
    };
}

@NotNull
@Override
public String getText() {
    StringBuilder sb = new StringBuilder();
    if (isIndented()) {
        sb.append("    ");
    }
    for (int i = 0; i < getChildren().size(); i++) {
        final WordNode word = getChildren().get(i);
        sb.append(word.getText());
        if (i != getChildren().size() - 1) {
            sb.append(" ");
        }
    }

    return sb.toString();
}

// -------------------------- PUBLIC METHODS --------------------------

public Style findDominatingStyle() {
    return TextUtils.findDominatingStyle(getChildren());
}

// -------------------------- OTHER METHODS --------------------------

private boolean isIndented() {
    if (getParent() == null) {
        return false;
    }

    final float paragraphX = getParent().getPos().getX();
    return getPos().getX() > paragraphX + 5.0f;//(float) findDominatingStyle().xSize * 2.0f;
}
}

/*
 * Copyright 2010 Øyvind Berg (elacin@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elacin.pdfextract.input;

import org.elacin.pdfextract.style.DocumentStyles;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: elacin
 * Date: 15.01.11
 * Time: 20.37
 * To change this template use File | Settings | File Templates.
 */
public class DocumentContent {
// ------------------------------ FIELDS ------------------------------

final DocumentStyles    styles = new DocumentStyles();
final List<PageContent> pages  = new ArrayList<PageContent>();

// --------------------- GETTER / SETTER METHODS ---------------------

public List<PageContent> getPages() {
    return pages;
}

public DocumentStyles getStyles() {
    return styles;
}

// -------------------------- PUBLIC METHODS --------------------------

public void addPage(PageContent page) {
    pages.add(page);
}
}

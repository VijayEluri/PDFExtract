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

package org.elacin.pdfextract;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.elacin.pdfextract.pdfbox.PDFTextStripper;
import org.elacin.pdfextract.tree.DocumentNode;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URL;

/**
 * Created by IntelliJ IDEA. User: elacin Date: May 9, 2010 Time: 12:19:56 AM To change this
 * template use File | Settings | File Templates.
 */
public class PDFDocumentLoader {

@NotNull
public static DocumentNode readPDF(String filename,
                                   final String outFile,
                                   final int endPage) throws IOException
{
	final URL url = PDFDocumentLoader.class.getClassLoader().getResource(filename);
	PDDocument document = PDDocument.load(url);
	PDFTextStripper stripper = new PDFTextStripper(document, -1, endPage);
	stripper.processDocument();

	final DocumentNode documentNode = stripper.getDocumentNode();
	documentNode.printTree(outFile, false);
	document.close();
	return documentNode;

}
}

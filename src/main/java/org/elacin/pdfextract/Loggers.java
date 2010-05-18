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

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * Created by IntelliJ IDEA.
 * User: elacin
 * Date: Apr 19, 2010
 * Time: 2:50:18 AM
 * To change this template use File | Settings | File Templates.
 */
public class Loggers {
    // ------------------------------ FIELDS ------------------------------

    private static final Logger createTreeLog = Logger.getLogger("org.elacin.pdfextract.createTree");
    private static final Logger pdfExtractorLog = Logger.getLogger("org.elacin.pdfextract");
    private static final Logger textNodeBuilderLog = Logger.getLogger("org.elacin.pdfextract.textNodeBuilder");

    static {
        PropertyConfigurator.configure(Loggers.class.getClassLoader().getResource("log4j.xml"));
    }

    // --------------------- GETTER / SETTER METHODS ---------------------

    public static Logger getCreateTreeLog() {
        return createTreeLog;
    }

    public static Logger getPdfExtractorLog() {
        return pdfExtractorLog;
    }

    public static Logger getTextNodeBuilderLog() {
        return textNodeBuilderLog;
    }

}

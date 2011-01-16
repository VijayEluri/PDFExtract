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

package org.elacin.pdfextract.geom;

/**
 * Created by IntelliJ IDEA. User: elacin Date: May 7, 2010 Time: 5:54:30 AM To change this template
 * use File | Settings | File Templates.
 */
public final class MathUtils {
// --------------------------- CONSTRUCTORS ---------------------------

private MathUtils() {
}

// -------------------------- PUBLIC STATIC METHODS --------------------------

/**
 * Returns true if num2 is within percentage percent of num1
 */
public static boolean isWithinPercent(final float num1, final float num2, final float percentage) {
    //noinspection FloatingPointEquality
    if (num1 == num2) {
        return true;
    }

    return (num1 + num1 / 100.0F * percentage) >= num2
            && (num1 - num1 / 100.0F * percentage) <= num2;
}

/**
 * Returns true if num2 is within num ± i
 */
public static boolean isWithinVariance(final float num1, final float num2, final float variance) {
    //noinspection FloatingPointEquality
    if (num1 == num2) {
        return true;
    }

    return (num1 - variance) <= num2 && (num1 + variance) >= num2;
}

@SuppressWarnings({"NumericCastThatLosesPrecision"})
public static float log(float a) {
    return (float) StrictMath.log((double) a);
}

@SuppressWarnings({"NumericCastThatLosesPrecision"})
public static float sqrt(float a) {
    return (float) StrictMath.sqrt((double) a);
}
}

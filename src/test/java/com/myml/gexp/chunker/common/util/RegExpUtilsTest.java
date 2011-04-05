package com.myml.gexp.chunker.common.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Author: java developer 1
 * Date: 12.03.11
 * Time: 15:37
 */
public class RegExpUtilsTest {
        @Test
        public void test() {
                String[] examples = new String[]{"javvva", "javggaaa", "javajava", "adsasd", "adasddsa"};
                String optimizedRegexp = RegExpUtils.convertListToRegexp(true, examples);
                Assert.assertEquals("(?:ad(?:asddsa|sasd)|jav(?:ajava|ggaaa|vva))", optimizedRegexp);
                for(String s : examples) Assert.assertTrue(s.matches(optimizedRegexp));
        }
}

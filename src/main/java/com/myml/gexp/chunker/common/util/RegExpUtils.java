package com.myml.gexp.chunker.common.util;

import com.google.common.collect.Lists;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Author: Yura Korolov
 * Date: 12.03.11
 * Time: 15:32
 */
public class RegExpUtils {
        private static char START = '^';
        private static char END = '$';

        public static String escapeRegexp(String str) {
                if (str == null) return null;
                final StringBuilder result = new StringBuilder();

                final StringCharacterIterator iterator =
                        new StringCharacterIterator(str);
                char character = iterator.current();
                while (character != CharacterIterator.DONE) {
                        /*
                                           * All literals need to have backslashes doubled.
                                           */
                        if (character == '.') {
                                result.append("\\.");
                        } else if (character == '\\') {
                                result.append("\\\\");
                        } else if (character == '?') {
                                result.append("\\?");
                        } else if (character == '*') {
                                result.append("\\*");
                        } else if (character == '+') {
                                result.append("\\+");
                        } else if (character == '-') {
                                result.append("\\-");
                        } else if (character == '&') {
                                result.append("\\&");
                        } else if (character == ':') {
                                result.append("\\:");
                        } else if (character == '{') {
                                result.append("\\{");
                        } else if (character == '}') {
                                result.append("\\}");
                        } else if (character == '[') {
                                result.append("\\[");
                        } else if (character == ']') {
                                result.append("\\]");
                        } else if (character == '(') {
                                result.append("\\(");
                        } else if (character == ')') {
                                result.append("\\)");
                        } else if (character == '^') {
                                result.append("\\^");
                        } else if (character == '|') {
                                result.append("\\|");
                        } else if (character == '$') {
                                result.append("\\$");
                        } else {
                                //the char is not a special one
                                //add it to the result as is
                                result.append(character);
                        }
                        character = iterator.next();
                }
                return result.toString();
        }

        public static String convertListToRegexp(final boolean useNonCapturingGroups, String... strs) {
                Arrays.sort(strs,
                        new Comparator<String>() {
                                public int compare(String o1, String o2) {
                                        int res = o2.length() - o1.length();
                                        if (res != 0) {
                                                return res;
                                        }
                                        return o1.compareTo(o2);
                                }
                        });

                class Node {
                        char ch = START;
                        List<Node> nodes = Lists.newArrayList();

                        void add(String str) {
                                if (str.length() == 0) return;
                                char chNew = str.charAt(0);
                                for (Node n : nodes) {
                                        if (n.ch == chNew) {
                                                n.add(str.substring(1));
                                                return;
                                        }
                                }
                                Node newNode = new Node();
                                newNode.ch = chNew;
                                newNode.add(str.substring(1));
                                nodes.add(newNode);
                        }

                        String toRegexp() {
                                StringBuilder str = new StringBuilder();
                                if (ch == START) {

                                } else if (ch == END) {

                                } else {
                                        String newStr = escapeRegexp(String.valueOf(ch));
                                        str.append(newStr);
                                }
                                if (nodes.size() > 1) {
                                        str.append(useNonCapturingGroups ? "(?:" : "(");
                                        for (Node n : nodes) {
                                                str.append("");
                                                str.append(n.toRegexp());
                                                str.append("|");
                                        }
                                        str.setLength(str.length() - 1);
                                        str.append(')');
                                } else if (nodes.size() == 1) {
                                        str.append(nodes.get(0).toRegexp());
                                }
                                return str.toString();
                        }
                }

                Node root = new Node();
                for (String str : strs) {
                        root.add(str + "$");
                }
                return root.toRegexp();
        }
}

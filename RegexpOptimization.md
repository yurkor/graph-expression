In this example i want to show how to create optimal Regexps for matching very big dictionaries.

Common example:
suppose we have city dictionaries with 10000 entries, you can convert it into regexp (City1|City2|City3|...|city10000) and it will try to check each of this word on every word in text... So on text size N words we will have N\*10000 matcher tryings.

In real word examples those dictionary regexp can be converted into much optimal regexp with log(N) complexity or even better(its depends on dictionary).

Here is example how it works:
```
//create array of your entries
String[] examples = new String[]{"javvva", "javggaaa", "javajava", "adsasd", "adasddsa"};
//convert them to optimal regexp
                String optimizedRegexp = RegExpUtils.convertListToRegexp(true, examples);
                Assert.assertEquals("(?:ad(?:asddsa|sasd)|jav(?:ajava|ggaaa|vva))", optimizedRegexp);
//check that it is works
                for(String s : examples) Assert.assertTrue(s.matches(optimizedRegexp));
```
package com.liferay.support.tools.portlet.actions.test

import com.liferay.support.tools.utils.CommonUtil
import com.liferay.support.tools.utils.RandomizeContentGenerator
import spock.lang.Specification
import spock.lang.Unroll

class RandomizeContentGeneratorTest extends Specification {
    RandomizeContentGenerator _rcg = null;

    def setup() {
        _rcg = new RandomizeContentGenerator();
        _rcg._commonUtil = new CommonUtil();
    }

    @Unroll("generateRandomParagraphes test")
    def "generateRandomParagraphes test"() {
        when:
        def retTxt = _rcg.generateRandomParagraphes(dbg_locale, sentenceCount)

        then:
        retTxt.size() == sentenceCount

        where:
        dbg_locale | sentenceCount
        "en"       | 10
    }

    @Unroll("generateRandomIndex test")
    def "generateRandomIndex test"() {
        when:
        List<Integer> idxs = _rcg.generateRandomIndex(totalParagraphs, randomAmount)

        then:
        idxs.size() == result
        for (int i; i < idxs.size() - 1; i++) {
            assert idxs[i] < idxs[i + 1];
        }

        where:
        totalParagraphs | randomAmount | result
        10              | 3            | 3
        10              | 11           | 10
    }

    @Unroll("insertRandomLinksInContents test <#param1> <#param2> <#param3> <#param4>")
    def "insertRandomLinksInContents_test"() {
        when:
        List<String> paragraphes = new ArrayList<>();
        param1.each { s ->
            paragraphes.add(s);
        }

        List<String> links = new ArrayList<>();
        param2.each { s ->
            links.add(s);
        }

        List<Integer> rndIndex = new ArrayList<>();
        param3.each { s ->
            rndIndex.add(s);
        }

        def result = _rcg.insertRandomLinksInContents(paragraphes, links, rndIndex)

        then:
        result != "";
        param4.each { seek ->
            assert true == result.contains(seek);
        }

        where:
        param1                            | param2                         | param3 | param4
        ["hoge", "fuga", "bar", "gegege"] | ["--aa--", "--bb--", "--cc--"] | [2]    | ["--cc--"]
        ["hoge", "fuga", "bar", "gegege"] | ["--aa--", "--bb--", "--cc--"] | [0, 2] | ["--aa--", "--cc--"]
        ["hoge", "fuga", "bar", "gegege"] | ["--aa--", "--bb--", "--cc--"] | [1, 2] | ["--bb--", "--cc--"]
        ["hoge", "fuga", "bar", "gegege"] | ["--aa--"]                     | [1, 2] | [""]
        ["hoge", "fuga", "bar", "gegege"] | ["--aa--"]                     | [0, 2] | ["--aa--"]
    }
}
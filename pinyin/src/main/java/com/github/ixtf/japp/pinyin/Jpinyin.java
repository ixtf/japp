package com.github.ixtf.japp.pinyin;

import com.github.ixtf.japp.core.J;
import net.sourceforge.pinyin4j.PinyinHelper;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author jzb 2018-08-17
 */
public class Jpinyin {

    public static boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS;
    }

    /**
     * @param cs 非中文字符被过滤
     * @return 中文 汉语拼音 简拼
     */
    public static Stream<String> pyAbbr(final CharSequence cs) {
        return Optional.ofNullable(cs)
                .filter(J::nonBlank)
                .flatMap(__ -> cs.chars()
                        .mapToObj(it -> (char) it)
                        .filter(Jpinyin::isChinese)
                        .map(PinyinHelper::toHanyuPinyinStringArray)
                        .filter(Objects::nonNull)
                        .map(it -> Arrays.stream(it)
                                /**
                                 * 取单字拼音的首字母
                                 */
                                .map(s -> s.substring(0, 1))
                                .distinct()
                        )
                        .reduce(Jpinyin::combinPy)
                )
                .map(Stream::distinct)
                .orElse(Stream.empty());
    }

    private static final Stream<String> combinPy(Stream<String> stream1, Stream<String> stream2) {
        Stream.Builder<String> builder = Stream.builder();
        Collection<String> set2 = stream2.collect(Collectors.toSet());
        stream1.forEach(a -> set2.forEach(b -> builder.add(a + b)));
        return builder.build();
    }

    /**
     * 中文 汉语拼音 全拼
     *
     * @param cs 非中文字符被过滤
     * @return 带声调，1 2 3 4 表示
     */
    public static Stream<String> pyFull(String cs) {
        return Optional.ofNullable(cs)
                .filter(J::nonBlank)
                .flatMap(__ -> cs.chars()
                        .mapToObj(it -> (char) it)
                        .filter(Jpinyin::isChinese)
                        .map(PinyinHelper::toHanyuPinyinStringArray)
                        .filter(Objects::nonNull)
                        .map(Arrays::stream)
                        .reduce(Jpinyin::combinPy)
                )
                .map(Stream::distinct)
                .orElse(Stream.empty());
    }
}

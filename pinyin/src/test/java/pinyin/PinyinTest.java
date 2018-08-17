package pinyin;

import com.github.ixtf.japp.pinyin.Jpinyin;
import com.google.common.collect.ImmutableSet;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class PinyinTest {
    private final static String word1 = "单.asdf,^dfs./\\][}{}，。！单单";
    private final static Set<String> result1;
    private final static String word2 = "单善若单";

    static {
        ImmutableSet.Builder builder = ImmutableSet.builder();
        builder.add("ccc");
        builder.add("css");
        builder.add("ssc");
        builder.add("sds");
        builder.add("dcd");
        builder.add("ccd");
        builder.add("ssd");
        builder.add("dcc");
        builder.add("dss");
        builder.add("ccs");
        builder.add("scc");
        builder.add("sss");
        builder.add("scd");
        builder.add("dcs");
        builder.add("scs");
        builder.add("ddc");
        builder.add("cdd");
        builder.add("cdc");
        builder.add("ddd");
        builder.add("csc");
        builder.add("dds");
        builder.add("sdd");
        builder.add("cds");
        builder.add("sdc");
        builder.add("dsd");
        builder.add("dsc");
        builder.add("csd");
        result1 = builder.build();
    }

    @Test
    public void test1() {
        Collection<String> set = Jpinyin.pyAbbr(word1)
                .peek(it -> Assert.assertTrue(result1.contains(it)))
                .collect(Collectors.toList());
        System.out.println(set.size());
        System.out.println(set);
        assertEquals(set.size(), result1.size());
        set = Jpinyin.pyFull(word1).collect(Collectors.toList());
        assertEquals(set.size(), result1.size());
    }

    @Test
    public void test2() {
        Collection<String> set = Jpinyin.pyFull(word1).collect(Collectors.toList());
        System.out.println(set.size());
        System.out.println(set);
        assertEquals(set.size(), result1.size());
        set = Jpinyin.pyAbbr(word2).collect(Collectors.toList());
        System.out.println(set.size());
        System.out.println(set);
        assertEquals(set.size(), 9);
        set = Jpinyin.pyFull(word2).collect(Collectors.toList());
        System.out.println(set.size());
        System.out.println(set);
        assertEquals(set.size(), 18);
    }

    @Test
    public void test3() {
        System.out.println(Number.class.isAssignableFrom(BigDecimal.class));
        System.out.println(Date.class.isAssignableFrom(Date.class));
    }
}

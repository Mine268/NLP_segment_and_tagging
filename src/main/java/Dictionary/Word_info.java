package Dictionary;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// 这个类描述了一个单词的信息
public class Word_info {

    public static class type_freq {
        public String type;
        public int freq;

        public type_freq(String vtype, int vfreq) {
            type = vtype;
            freq = vfreq;
        }
    }

    // 储存了这个词语的每个词性出现的频率
    protected List<type_freq> type_freqList;

    public Word_info(int length) {
        type_freqList = new ArrayList<>(length);
    }

    /**
     * 增添这个词的词性。如果没有则添加上，如果有则修改其频率。
     * @param wi 词性信息
     */
    public void append_type(Word_info wi) {
        for (var tf : wi.type_freqList) {
            var it = type_freqList.iterator();
            type_freq context = null;
            boolean find = false;
            while (it.hasNext()) {
                context = it.next();
                // do `=` not `==`
                if (find = tf.type.equals(context.type))
                    break;
            }

            if (find) {
                context.freq += tf.freq;
            } else {
                add_type(tf.type, tf.freq);
            }
        }
    }

    public Word_info add_type(String type, int freq) {
        type_freqList.add(new type_freq(type, freq));
        return this;
    }

    /**
     * 获取这个词作为词性 {@code t} 出现的频率
     * @param t 词性
     * @return 频率
     */
    public int get_freq_of_type(String t) {
        return type_freqList.stream()
                .filter(x -> x.type.equals(t))
                .map(x -> x.freq)
                .reduce(0, Integer::sum);
    }

    public int get_type_count() {
        return type_freqList.size();
    }

    /**
     * 返回该词语的频率和
     * @return 频率和
     */
    public int get_type_sum_by_freq() {
        return type_freqList.stream()
                .map(x -> x.freq)
                .reduce(0, Integer::sum);
    }

    /**
     * 返回一个列表的流
     * @return 流
     */
    public Stream<type_freq> stream() {
        return type_freqList.stream();
    }

    @Override
    public String toString() {
        return String.format("%s",
                type_freqList.stream()
                        .map(x -> String.format("%s %d", x.type, x.freq))
                        .collect(Collectors.toList())
        );
    }
}

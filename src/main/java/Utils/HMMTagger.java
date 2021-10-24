package Utils;

import Dictionary.Word;
import Dictionary.Word_info;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 隐式马尔科夫词性标注模型
 */
public class HMMTagger {

    // 指示模型是否准备完成，准备完成之后不可以继续增加语料（因为ACTrie是这么弄的）
    boolean prepared;
    // 词库，使用了{@link ACTrie}模型
    private final ACTrie dictionary;
    // 词性统计，统计每一种词性出现在字典中的词的数量
    private final Map<String, Integer> part_sum;
    // 词性转移统计
    private final Map<String, Map<String, Integer>> part_trans;

    public HMMTagger() {
        prepared = false;
        dictionary = new ACTrie();
        part_sum = new HashMap<>();
        part_trans = new HashMap<>();
    }

    /**
     * 构造时加载语料库
     * @param corpus_path 语料库地址
     */
    public HMMTagger(String corpus_path) {
        prepared = false;
        dictionary = new ACTrie();
        part_sum = new HashMap<>();
        part_trans = new HashMap<>();
        try {
            FileReader file = new FileReader(corpus_path);
            BufferedReader buf = new BufferedReader(file);
            while (buf.ready())
                insert_line(buf.readLine());
        } catch (Exception e) {
            e.printStackTrace();
        }
        prepared = true;
    }

    /**
     * 分析并插入一条语料，这会向{@code dictionary}中插入语料的组成词语，并向{@code part_sum}中的对应词性进行增加。
     * @param line 语料行
     */
    public void insert_line(String line) {
        if (prepared) {
            System.err.println(this + "不能再添加语料。");
            return;
        }

        // start, end 进行词语的切片，范围为 [start, end-1]
        int start = 0, end = 1;
        while (start < line.length()) {
            if (line.charAt(start) != '[') {
                // 进行词语的添加
                while (end < line.length() && line.charAt(end) != ' ')
                    ++end;
                int slash_index = end - 1;
                while (line.charAt(slash_index) != '/')
                    --slash_index;

                var word = line.substring(start, slash_index);
                var part = line.substring(slash_index + 1, end);

                dictionary.add_word(new Word(word, new Word_info(1).add_type(part, 1)));

                // 进行词性的统计

                // 进行词性转移的统计

            } else { // 嵌套标注的词语
                // 进行词语的添加
                while (end < line.length() && line.charAt(end) != ']')
                    ++end;

                var sub_line = line.substring(start + 1, end);

                // 处理嵌套
                insert_line(sub_line);

                var merge_word = Arrays.stream(sub_line.split("\\s"))
                        .map(x -> x.substring(0, x.indexOf("/")))
                        .reduce("", String::concat);
                var end_end = ++end;
                end_end =  ++end;
                while (end_end < line.length() && line.charAt(end_end) != ' ')
                    ++end_end;
                var merge_part = line.substring(end, end_end);

                dictionary.add_word(new Word(merge_word, new Word_info(1).add_type(merge_part, 1)));
                end = end_end;
            }
            start = ++end;
            end = start + 1;

            // 进行词性的统计

            // 进行词性转移的统计
        }
    }

    /**
     * 输出以下字典中的所有存储的词语。
     * @param stringer 输出节点信息的接口
     */
    public void print_dictionary(ACTrie.ACTrie_node.ACTrie_node_stringer stringer) {
        dictionary.travel_through(stringer);
    }

}

package Utils;

import Dictionary.Word;
import Dictionary.Word_info;
import Dictionary.Segment;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 隐式马尔科夫词性标注模型，对于没有标注的词语默认其词性为w。UTF8编码。
 * @author mine268
 * @version 1.0
 */
public class HMMTagger {

    // 指示模型是否准备完成，准备完成之后不可以继续增加语料（因为ACTrie是这么弄的）
    private boolean prepared;
    // 词库，使用了ACTrie模型
    private final ACTrie dictionary;
    // 词性统计，统计每一种词性出现在字典中的词的数量
    private final Map<String, Integer> part_sum;
    // 统计每一种词性转移出去的词频的数量
    private final Map<String, Integer> part_trans_sum_freq;
    // 词性转移统计
    private final Map<String, Map<String, Integer>> part_trans;
    // 每一个词性拥有的词语的种类数量的统计
    private final Map<String, Integer> part_word_count;
    // 匹配未标记的符号
    private static final Pattern regex_comm = Pattern.compile("[：。？！“”【】‘’、，；（）:.?!\"',;()-]|…{2}");
    // 匹配多余的连续空格
    private static final Pattern regex_space = Pattern.compile("\\s{2,}");

    // 用于浮点比较的常数
    private static final float epsilon = 1e-4f;
    // 概率线性插值参数
    private static final float lambda = 0.8f;

    public HMMTagger() {
        prepared = false;
        dictionary = new ACTrie();
        part_sum = new HashMap<>();
        part_trans_sum_freq = new HashMap<>();
        part_trans = new HashMap<>();
        part_word_count = new HashMap<>();
    }

    /**
     * 构造时加载语料库，语料库的编码类型为UTF8。
     * @param corpus_path 语料库地址
     */
    public HMMTagger(String corpus_path) {
        prepared = false;
        dictionary = new ACTrie();
        part_sum = new HashMap<>();
        part_trans_sum_freq = new HashMap<>();
        part_trans = new HashMap<>();
        part_word_count = new HashMap<>();

        try {
            FileReader file = new FileReader(corpus_path);
            BufferedReader buf = new BufferedReader(file);
            int line_count = 0;
            while (buf.ready()) {
                var line = buf.readLine();
                try {
                    insert_line(line);
                    ++line_count;
                } catch (Exception e) {
                    System.out.printf("第%d行语料解析失败，内容：%s...\n", line_count, line.substring(0, 50));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        buildup();
    }

    /**
     * 进行dictionary的buildup，完成之后不能再继续添加语料库。
     */
    public void buildup() {
        if (!prepared) {
            dictionary.buildup();
            prepared = true;
        }
        // 临时变量置空
        _part_text.clear();
        _part_text = null;
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

        // 进行预处理
        var processed_line = _clean_line(line.trim());
        // 分析包含的词语和统计词性
        _analysis_word(processed_line);
        // 统计这一语料行的转移频率
        _analysis_transmission(processed_line);

    }

    /**
     * 使用Viterbi算法对这个分词结果进行词性标注
     * @param seg 进行词性标注的分词
     * @return 进行过词性标注的分词结果
     */
    final public List<String> tag(final Segment seg) {
        List<String> res = new ArrayList<>(seg.length());

        // 词性列表
        var part_list = new ArrayList<>(part_sum.keySet());
        // 每一层节点拥有的节点数量
        int lyr_count = seg.length();
        int lyr_size = part_list.size();
        // path_length[i][j] 为从第一层到第 i 层第 j 个节点的最长路径
        float[][] path_length = new float[lyr_count][lyr_size];
        // previous[i][j] 为从第一层到第 i-1 层第 j 个节点的最长路径上的前驱，首层不存储
        int[][] previous = new int[lyr_count - 1][lyr_size];

        // 初始化第一个词语
        for (int i = 0; i < lyr_size; ++i) {
            path_length[0][i] = characteristic_collapse_probability(part_list.get(i), seg.get_which(0).word);
        }

        // dp
        for (int i = 1; i < lyr_count; ++i) {
            for (int j = 0; j < lyr_size; ++j) { // 遍历第 i 层
                float tmp_max = -Float.MAX_VALUE;
                int tmp_prev = -1;
                for (int k = 0; k < part_list.size(); ++k) { // 查找第 i-1 层
                    float tmp_val = path_length[i - 1][k]
                            * transmission_probability(part_list.get(k), part_list.get(j))
                            * characteristic_collapse_probability(part_list.get(j), seg.get_which(i).word);
                    if (tmp_val > tmp_max) {
                        tmp_max = tmp_val;
                        tmp_prev = k;
                    }
                }
                path_length[i][j] = tmp_max;
                previous[i - 1][j] = tmp_prev;
            }
        }

        int cur_ind = 0;
        float tmp_max = Float.MIN_VALUE;
        for (int i = 0; i < lyr_size; ++i)
            if (tmp_max < path_length[lyr_count - 1][i]) {
                tmp_max = path_length[lyr_count - 1][i];
                cur_ind = i;
            }

        Stack<String> res_stack = new Stack<>();
        for (int i = lyr_count - 2; i >= 0; --i) {
            res_stack.push(part_list.get(cur_ind));
            cur_ind = previous[i][cur_ind];
        }
        res_stack.push(part_list.get(cur_ind));
        while (!res_stack.empty())
            res.add(res_stack.pop());

        return res;
    }

    /**
     * 返回词性转移的概率，按照0.8的参数进行线性插值平滑
     * @param former_part 前一个词性
     * @param latter_part 后一个词性
     * @return 概率
     */
    private float transmission_probability(final String former_part, final String latter_part) {
        float p1_ml = ((float) part_sum.get(latter_part) + 1) /
                (dictionary.vocal_count() + part_sum.size());

        // 如果找不到对应的转移，返回 null，这时应该手动置为 0
        int trans_freq;
        var trans_freq_tree = part_trans.get(former_part);
        if (trans_freq_tree == null)
            trans_freq = 0;
        else {
            var trans_freq_tree_val = trans_freq_tree.get(latter_part);
            trans_freq = trans_freq_tree_val == null ? 0 : trans_freq_tree_val;
        }

        var ptsf = part_trans_sum_freq.get(former_part);
        var pt_tree = part_trans.get(former_part);
        ptsf = ptsf == null ? 0 : ptsf;
        var pt = pt_tree == null ? 0 : pt_tree.size();
        float p2_ml = ((float) trans_freq + 1) / (ptsf + pt);

        p2_ml = Float.isFinite(p2_ml) ? p2_ml : 0.f;

        return p2_ml * lambda + p1_ml * (1 - lambda);
    }

    /**
     * 获取当前词性表现为特定词语的概率，这个概率使用了+1法进行平滑
     * @param part 词性
     * @param word 词语
     * @return 返回概率
     */
    private float characteristic_collapse_probability(final String part, final Word word) {
        var w_info = dictionary.get_info_of_word(word.text);
        int w_freq = (w_info == null ? 0 : w_info.info.get_freq_of_type(part));
        int total = (w_info == null ? 1 : w_info.info.get_type_sum_by_freq());

        return ((float) (w_freq + 1)) / (total + part_word_count.get(part));
    }

    /**
     * 分析并插入一条语料，这会向{@code dictionary}中插入语料的组成词语，并向{@code part_sum}中的对应词性进行增加。
     * @param line 语料行
     */
    private void _analysis_word(String line) {
        // start, end 进行词语的切片，范围为 [start, end-1]
        int start = 0, end = 1;
        while (start < line.length()) {
            if (line.charAt(start) != '[' || line.charAt(start + 1) == '/') {
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
                _increase_part(word, part);
            } else { // 嵌套标注的词语
                // 进行词语的添加
                while (end < line.length() && line.charAt(end) != ']')
                    ++end;

                var sub_line = line.substring(start + 1, end);

                // 处理嵌套
                _analysis_word(sub_line);

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

                // 进行词性的统计
                _increase_part(merge_word, merge_part);

            }
            start = ++end;
            end = start + 1;
        }
    }

    /**
     * 分析词性转移频率
     * @param line 句子
     */
    private void _analysis_transmission(String line) {
        int start = 0, end = 0;
        List<String> part_list = new LinkedList<>();

        while (end < line.length()) {
            if (line.charAt(end) == '[' && line.charAt(end + 1) != '/') {
                while (line.charAt(end) != ']')
                    ++end;
            }
            while (end < line.length() && line.charAt(end) != ' ')
                ++end;

            start = end - (end == line.length() ? 1 : 0);
            while (line.charAt(start) != '/')
                --start;
            ++start;

            part_list.add(line.substring(start, end));
            ++end;
        }

        var former = part_list.iterator();
        var latter = part_list.iterator();

        latter.next();
        while(latter.hasNext())
            _increase_trans(former.next(), latter.next());
    }

    /**
     * 对句子进行校对。 <br/>
     * 没有正确标注的标点符号赋予词性 {@code w}<br/>
     * TODO: 两阶段的字符替换可以合并成一次<br/>
     * TODO: 正则表达的应用式可以简化，这里为了方便使用了正则
     * @param line 进行校对的句子
     * @return 校对之后的结果
     */
    private static String _clean_line(String line) {
        StringBuilder result_str = new StringBuilder(line);
        var matcher_comm = regex_comm.matcher(line);
        var results_comm = matcher_comm.results().collect(Collectors.toList());
        var it_comm = results_comm.listIterator(results_comm.size());

        while (it_comm.hasPrevious()) {
            var token = it_comm.previous();
            if (token.end() >= line.length() || line.charAt(token.end()) != '/')
                result_str.replace(token.start(), token.end(),
                        line.substring(token.start(), token.end()) + "/w ");
        }

        var matcher_space = regex_space.matcher(result_str);
        var results_space = matcher_space.results().collect(Collectors.toList());
        var it_space = results_space.listIterator(results_space.size());

        while (it_space.hasPrevious()) {
            var token = it_space.previous();
            result_str.replace(token.start(), token.end(), " ");
        }

        return result_str.toString();
    }

    /**
     * 对应词性增加1，同时修改每种词性的词语数量
     * @param part 词性
     */
    private void _increase_part(String word, String part) {
        if (!part_sum.containsKey(part)) {
            // 该词性以前未被统计过
            // 该词性的数量设置为 1
            part_sum.put(part, 1);
            // 这个词性下出现了词语 word
            Set<String> tmpSet = new HashSet<>();
            tmpSet.add(word);
            _part_text.put(part, tmpSet);
            // 这个词性下词语的数量设置为 1
            part_word_count.put(part, 1);
        } else {
            part_sum.put(part, part_sum.get(part) + 1);
            var wordSet = _part_text.get(part);
            if (!wordSet.contains(word)) {
                wordSet.add(word);
                part_word_count.put(part, part_word_count.get(part) + 1);
            }
        }
    }
    // 这是个临时变量，用于记录某一词性下是否已经统计了某一词语
    private Map<String, Set<String>> _part_text = new HashMap<>();

    /**
     * 进行一个词性转移的统计
     * @param former 前一个词语的词性
     * @param latter 后一个词语的词性
     */
    private void _increase_trans(String former, String latter) {
        if (!part_trans.containsKey(former)) {
            part_trans.put(former, new HashMap<>());
            part_trans_sum_freq.put(former, 1);
        } else {
            part_trans_sum_freq.put(former, part_trans_sum_freq.get(former) + 1);
        }

        var temp = part_trans.get(former);
        if (!temp.containsKey(latter))
            temp.put(latter, 1);
        else
            temp.put(latter, temp.get(latter) + 1);
    }

    /**
     * 输出以下字典中的所有存储的词语。
     * @param stringer 输出节点信息的接口
     */
    public void print_dictionary(ACTrie.ACTrie_node.ACTrie_node_stringer stringer) {
        dictionary.travel_through(stringer);
    }
}

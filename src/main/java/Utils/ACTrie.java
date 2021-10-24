package Utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import Dictionary.*;

/**
 * AC自动机，添加词语并进行建构后不可删除，用于在一个字符串上一次匹配多个字符，添加词语通过方法{@code add_word}或{@code add_words}或构造方法
 * {@code ACTrie(List<Word> words)}进行，构建通过方法{@code buildup}进行。<br/>
 * TODO: 所有的containKey和get对可以优化
 * @author mine268
 * @version 1.0
 */
public class ACTrie {
    /**
     * AC自动机中的节点
     * @version 1.0
     */
    public static class ACTrie_node {
        /**
         * 这是一个用于展示节点信息的接口
         */
        public interface ACTrie_node_stringer {
            String fetchString(String key, ACTrie_node node);
        }
        // 子节点
        public Map<String, ACTrie_node> next;
        // fail边
        public ACTrie_node fail;
        // fail边连接到这个节点的节点
        public Set<ACTrie_node> fail_inv;
        // 用于保存宽度优先搜索曾在第几步的时候抵达了这个节点
        protected List<Integer> reach_index;
        // 指示该节点是否可以作为词语结尾
        public boolean terminate;
        // 如果有，那么则看一下当前词语对应的信息
        public Word word;
        // 节点深度
        protected int depth;

        /**
         * 常规的构造函数
         */
        public ACTrie_node() {
            next = new HashMap<>();
            fail = null;
            fail_inv = new HashSet<>();
            reach_index = new LinkedList<>();
            terminate = false;
            word = null;
            depth = 0;
        }

        /**
         * 重置节点状态，即重置reach_index
         */
        public void reset_reach_index() {
            reach_index.clear();
        }
    }

    // 作为AC自动机的根节点
    private final ACTrie_node root;
    // 总词数（不包含词频）
    private int total_vocab;
    // 指示是否可以添加词语，false表示可以，true表示不可以，因为此时已经建立了fail边
    private boolean change_lock;

    /**
     * 常规的构造函数，构造的AC自动机可以使用{@code add_word}和{@code add_words}手动添加词语
     */
    public ACTrie() {
        change_lock = false;
        root = new ACTrie_node();
        root.fail = root;
        total_vocab = 0;
    }

    /**
     * 以当前提供的词语完成对AC自动机的构造，完成构造之后不能继续添加词语
     * @param words 提供的词语
     */
    public ACTrie(List<Word> words) {
        change_lock = false;
        root = new ACTrie_node();
        root.fail = root;
        total_vocab = 0;

        // 把`words`中的所有词语添加到AC自动机中
        add_words(words);
        buildup();

        change_lock = true;
    }

    /**
     * 通过字典文件构建AC自动机
     * @param path 字典文件的地址
     */
    public ACTrie(String path) {
        change_lock = false;
        root = new ACTrie_node();
        root.fail = root;
        total_vocab = 0;

        FileReader dict_reader = null;
        try {
            dict_reader = new FileReader(path);
            BufferedReader buf_reader = new BufferedReader(dict_reader);

            // 层层读入
            while (buf_reader.ready()) {
                var line = buf_reader.readLine();
                var line_split = line.split("\t");
                var word = new Word(line_split[0]);

                for (int i = 1; i < line_split.length; i += 2)
                    word.info.add_type(line_split[i], Integer.parseInt(line_split[i + 1]));

                add_word(word);
            }
            buildup();

        } catch (IOException e1) {
            e1.printStackTrace();
        } finally {
            if (dict_reader != null)
                try {
                    dict_reader.close();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
        }
    }

    /**
     * 向AC自动机中添加一条词语word，添加后的词语形成trie树，必须运行buildup方法才能完成AC自动机的构建。<br><br/>
     * 如果添加的词语已经添加过，那么就会进行词性的增补。
     * @param word 要添加的词语
     * @return 如果添加成功，则为当前ACTrie的引用；否则为空引用
     */
    public ACTrie add_word(Word word) {
        // 向AC自动机中添加词语word
        // 添加词语的步骤有二，第一步从根节点向下查找词语是否存在，如果存在那么则替换词性，否则新增节点（如果必要），然后在最后节点上建立terminate，
        // 然后向前回溯建立fail边

        // #0 检查change_lock是否为false，即是否可以添加词语
        if (change_lock) {
            System.err.println(this + " 已完成buildup，已不可添加词语，[" + word + "]添加失败！");
            return null;
        }

        // #1 查找要插入的词语是否存在
        ACTrie_node current = root;
        int index = 0;
        while (index < word.text.length()) {
            String on_word = String.valueOf(word.text.charAt(index));
            if (current.next.containsKey(on_word))
                current = current.next.get(on_word);
            else
                break;

            ++index;
        }

        // #2 开始新增边
        while (index < word.text.length()) {
            String on_word = String.valueOf(word.text.charAt(index));
            ACTrie_node next_node = new ACTrie_node();
            // 这里我们建立了当前新字对应下一个节点
            current.next.put(on_word, next_node);
            next_node.depth = current.depth + 1;

            current = next_node;
            ++index;
        }

        // #3 设置terminate和w_info
        if (!current.terminate)
            // 总词数加一
            total_vocab += 1;
        current.terminate = true;
        if (current.word == null)
            current.word = word.copy();
        else
            current.word.append_info(word.info);

        return this;
    }

    /**
     * 向AC自动机中添加多条词语形成trie树，添加完成之后必须运行buildup方法才完成了AC自动机构造
     * @param words 词语集合
     */
    public ACTrie add_words(List<Word> words) {
        // 向AC自动机中添加若干词语，通过调用add_word实现
        if (change_lock) {
            System.err.println(this + " 已不可添加词语，"
                    + words.stream().limit(10).collect(Collectors.toList()) + " 添加失败！");
        } else {
            for (var word : words)
                add_word(word);
        }
        return this;
    }

    /**
     * 运行这个方法将完成AC自动机的构建，即建立fail边，建立后不可以再继续添加词语
     */
    public void buildup() {
        // 建立fail边
        if (!change_lock) {
            Queue<ACTrie_node> bfs_queue = new ArrayDeque<>();
            bfs_queue.add(root);

            while (!bfs_queue.isEmpty()) {
                var current = bfs_queue.poll();
                for (var next : current.next.entrySet()) {
                    var key = next.getKey();
                    var node = next.getValue();
                    var fail = current.fail;
                    boolean flag;
                    while (!(flag = fail.next.containsKey(key))
                            && fail != root)
                        fail = fail.fail;
                    var res = fail.next.get(key);
                    if (flag && res != node) {
                        node.fail = res;
                        res.fail_inv.add(node);
                    } else {
                        node.fail = root;
                        root.fail_inv.add(node);
                    }

                    bfs_queue.add(node);
                }
            }

            change_lock = true;
        }
    }

    /**
     * 广度遍历输出整棵树，第一个输出的节点是root节点
     * @param str_func 输出每个节点信息的函数
     */
    public void travel_through(ACTrie_node.ACTrie_node_stringer str_func) {
        System.out.println(str_func.fetchString("$$", root));
        Queue<ACTrie_node> bfs_queue = new ArrayDeque<>();
        bfs_queue.add(root);
        while (!bfs_queue.isEmpty()) {
            var top = bfs_queue.poll();
            top.next.forEach((key, val) -> {
                bfs_queue.add(val);
                System.out.println(str_func.fetchString(key, val));
            });
        }
    }

    /**
     * 对字符串进行模式匹配，返回所有字典中出现了的词语的位置
     * @param str 要进行匹配的字符串
     * @return 返回出现了的词语的位置
     */
    public List<Seg_unit> match_patterns_for(String str) {
        //  利用AC自动机对串str进行模式匹配，返回匹配得到的串
        var current = root;
        var string = str.toCharArray();

        // 表示当前处理的字符串是从0~cur_ind-1的位置
        for (int cur_ind = 0; cur_ind < string.length; ++cur_ind) {
            var on_char = String.valueOf(string[cur_ind]);
            if (current.next.containsKey(on_char)) {
                current = current.next.get(on_char);
            } else {
                boolean flag;
                current = current.fail;
                while (!(flag = current.next.containsKey(on_char))
                        && current != root)
                    current = current.fail;
                if (flag)
                    current = current.next.get(on_char);
                // else assert current == root
            }
            current.reach_index.add(cur_ind);
        }

        return collect_result();
    }

    /**
     * 从根节点角度获得结果
     * @return 统计得到的结果
     */
    private List<Seg_unit> collect_result() {
        var result = collect_result_on(root);
        clear_reach_index();
        return result;
    }

    /**
     * 清空所有节点上的match index
     */
    private void clear_reach_index() {
        Queue<ACTrie_node> bfs_queue = new ArrayDeque<>();
        bfs_queue.add(root);
        while (!bfs_queue.isEmpty()) {
            var top = bfs_queue.poll();
            top.reset_reach_index();
            bfs_queue.addAll(top.next.values());
        }
    }

    /**
     * 递归地统计最终结果
     * @param node 依据哪一个节点统计
     * @return 统计到的结果
     */
    private List<Seg_unit> collect_result_on(ACTrie_node node) {
        if (node.fail_inv.isEmpty()) {
            if (!node.terminate)
                return new LinkedList<>();
            else
                return node.reach_index.stream()
                        .map(end -> new Seg_unit(node.word, end - node.word.text.length() + 1))
                        .collect(Collectors.toList());
        } else {
            var result = node.fail_inv.stream()
                    .map(this::collect_result_on)
                    .reduce(new LinkedList<>(), (res, ele) -> { res.addAll(ele); return res; });
            if (node.terminate)
                node.reach_index
                        .forEach(end -> result.add(new Seg_unit(node.word, end - node.word.text.length() + 1)));
            return result;
        }
    }

    /**
     * 使用AC自动机进行分词操作，基于正向最长匹配，已排序
     * @param text 进行分词的句段
     * @return 分词结果
     */
    public Segment forward_segment_sentence(String text) {
        var result_seg = new Segment();
        var words = match_patterns_for(text);
        if (words.size() == 0)
            return result_seg;

        // 按照index排序，方便生成segment
        words.sort((x, y) -> {
            if (x.index != y.index)
                return x.index - y.index;
            else
                return y.word.text.length() - x.word.text.length();
        });
        var it = words.iterator();
        var curr_seg = it.next();
        int first_index = 0;

        do {
            if (first_index < curr_seg.index) {
                result_seg.add_segment(
                        new Seg_unit(new Word(text.substring(first_index, first_index + 1)), first_index));
                ++first_index;
            } else {
                result_seg.add_segment(curr_seg);
                first_index += curr_seg.word.text.length();

                while (it.hasNext() && curr_seg.index < first_index)
                    curr_seg = it.next();
            }
        } while (first_index < text.length() && it.hasNext());

        while (first_index < text.length()) {
            result_seg.add_segment(
                    new Seg_unit(new Word(text.substring(first_index, first_index + 1)), first_index)
            );
            ++first_index;
        }

        return result_seg;
    }

    /**
     * 使用AC自动机进行分词操作，已排序，基于最少词语的原则，具体如下<br/>
     * 首先进行完全分词，获得每一个分词的结果，从中选取不相交的词语使得分词的数量加上没有得到分词的字的数量之和最小
     * @param text 进行分词的句段
     * @return 分词结果
     */
    public Segment shortest_segment_sentence(String text) {
        // 每一个元素表示一个节点，上面的边表示以这个字开头的词语
        List<List<Seg_unit>> edges = new ArrayList<>(text.length());
        for (int i = 0; i < text.length(); ++i)
            edges.add(new LinkedList<>());
        var segment = match_patterns_for(text);
        segment.forEach(ele -> edges.get(ele.index).add(ele) );

        // 考虑0~i-1长度的句子，最后一个分词对应path_seg[i]
        var path_seg = new Seg_unit[text.length() + 1];
        // 考虑0~i-1长度的句子，其最短分词长度为path_length[i]
        var path_length = new int[text.length() + 1];
        path_seg[1] = null;
        path_length[1] = 1;
        for (int i = 2; i <= text.length(); ++i) {
            int tmp_len = Integer.MAX_VALUE;
            Seg_unit tmp_seg = null;
            Optional<Seg_unit> edge;
            for (int j = 0; j < i; ++j) {
                int finalI = i;
                int finalJ = j;
                edge = edges.get(j).stream()
                        .filter(e -> finalJ + e.word.text.length() == finalI)
                        .findFirst();
                if ((i - j == 1 || edge.isPresent())
                        && tmp_len > path_length[j] + 1) {
                    tmp_len = path_length[j] + 1;
                    tmp_seg = edge.orElse(null);
                }
            }
            path_length[i] = tmp_len;
            path_seg[i] = tmp_seg;
        }

        // 逆序输出，即Segment里头的seg_unit是正序的
        Stack<Seg_unit> result_stack = new Stack<>();
        for (int i = text.length(); i > 0;) {
            if (path_seg[i] == null) {
                result_stack.push(new Seg_unit(new Word(text.substring(i - 1, i)), i - 1));
                --i;
            } else {
                result_stack.push(path_seg[i]);
                i -= path_seg[i].word.text.length();
            }
        }
        Segment result_seg = new Segment(result_stack.size());
        while (!result_stack.empty())
            result_seg.add_segment(result_stack.pop());

        return result_seg;
    }

    /**
     * 返回AC自动机中词语的数量
     * @return 词语的数量
     */
    public int vocal_count() {
        return total_vocab;
    }
}

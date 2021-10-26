package Utils;

import Dictionary.Seg_unit;
import Dictionary.SegmentDictionary;
import Dictionary.Word;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 用于评测分词结果的类
 */
public class SegmentGrader {

    // 文件的地址
    private String file_path;
    // 要读取的文件
    private FileReader file_reader;
    // 用于按行读取
    private BufferedReader buf_reader;

    // 指示统计分类结果的时候是否考虑词性
    private boolean consider_partOfSpeech;

    /**
     * 分词结果的记录类
     */
    public static class Result {
        // 准确率
        final public float precision;
        // 召回率
        final public float recall;
        // f1值
        final public float f1;

        /**
         * 构造函数，用于构造参数
         * @param precision 准确率
         * @param recall 召回率
         * @param f1 f1值
         */
        public Result(float precision, float recall, float f1) {
            this.precision = precision;
            this.recall = recall;
            this.f1 = f1;
        }

        @Override
        public String toString() {
            return String.format("p: %.3f, r: %.3f, f1: %.3f", precision, recall, f1);
        }
    }

    /**
     * 默认初始化为{@code null}
     */
    public SegmentGrader() {
        file_reader = null;
        buf_reader = null;
        consider_partOfSpeech = false;
    }

    /**
     * 指定目标分词结果进行一个读取
     * @param path 给定的分词结果文件
     */
    public SegmentGrader(String path) {
        consider_partOfSpeech = false;
        try {
            file_reader = new FileReader(path);
            buf_reader = new BufferedReader(file_reader);
            file_path = path;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 加载目标分词文件，如果加载成功则返回{@code true}，否则为{@code false}并保持原有加载的文件不变
     * @param path 要加载的目标路径
     * @return 是否加载成功
     */
    public boolean load_file(String path) {
        var old_file_reader = file_reader;
        var old_buf_reader = buf_reader;
        try {
            file_reader = new FileReader(path);
            buf_reader = new BufferedReader(file_reader);
            old_buf_reader.close();
            old_file_reader.close();
            file_path = path;
            return true;
        } catch (FileNotFoundException e) {
            // 如果新文件打开失败，则维持旧文件
            e.printStackTrace();
            file_reader = old_file_reader;
            buf_reader = old_buf_reader;
            return false;
        } catch (IOException e) {
            // 如果旧文件关闭失败，则重新打开旧文件
            e.printStackTrace();
            try {
                file_reader = new FileReader(file_path);
                buf_reader = new BufferedReader(file_reader);
            } catch (FileNotFoundException e1) {
                // 旧文件打开失败，直接清空，资源的释放工作忽略，由jvm接手
                e.printStackTrace();
                file_reader = null;
                buf_reader = null;
                file_path = "";
            }
            return false;
        }
    }

    /**
     * 在整个分词答案上验证分词结果
     * @param dict 分词器
     * @return 分词指标
     */
    public Result getResult(SegmentDictionary dict) {
        return getResult(dict, false, 0);
    }

    /**
     * 在整个分词答案的前{@code line_limie}行上验证分词结果
     * @param dict 分词器
     * @param line_limit 前几行
     * @return 分词指标
     */
    public Result getResult(SegmentDictionary dict, int line_limit) {
        return getResult(dict, true, line_limit);
    }

    /**
     * 获取分词准确率信息
     * @param dict 使用哪一个词典的哪一种方法进行分词
     * @param line_limit 只参考前多少行进行评估
     * @param do_limit 是否限制查验的行数
     * @return 分词指标
     */
    private Result getResult(SegmentDictionary dict, boolean do_limit, int line_limit) {
        // 不考虑词性的分词方式
        if (!consider_partOfSpeech) {
            // TODO: 此处代码默认分词结果文件没有词性标注
            if (!(file_path.equals("") || file_reader == null || buf_reader == null)) {
                try {
                    int A = 0, B = 0, A_cap_B = 0;
                    while (buf_reader.ready() && (!do_limit || --line_limit >= 0)) {
                        var temp_begin = new AtomicInteger(0);
                        var line_array = buf_reader.readLine().split("\\s+");
                        // 正确的分词结果数组
                        var golden_sep = Arrays.stream(line_array)
                                .map(x -> new Seg_unit(new Word(x), temp_begin.addAndGet(x.length())))
                                .collect(Collectors.toList());
                        // 这个句子经过dict得到的分词结果
                        var test_sep = dict.segment(Arrays.stream(line_array)
                                .reduce("", String::concat)).get_raw();

                        // 当前索引到的目标字符串中的位置
                        int golden_index = 0;
                        int test_index = 0;
                        // 当前匹配到的词语的迭代器
                        var golden_it = golden_sep.iterator();
                        var test_it = test_sep.iterator();

                        A += golden_sep.size();
                        B += test_sep.size();
                        while (golden_it.hasNext() && test_it.hasNext()) {
                            if (golden_index < test_index)
                                golden_index += golden_it.next().word.text.length();
                            else if (golden_index > test_index)
                                test_index += test_it.next().word.text.length();
                            else {
                                var gn = golden_it.next();
                                var tn = test_it.next();
                                golden_index += gn.word.text.length();
                                test_index += tn.word.text.length();

                                if (gn.word.text.equals(tn.word.text))
                                    ++A_cap_B;
                            }
                        }
                    }
                    float p = A_cap_B / (float) B;
                    float r = A_cap_B / (float) A;
                    float f1 = 2 * p * r / (p + r);
                    return new Result(p, r, f1);
                } catch(IOException e) {
                    e.printStackTrace();
                    return null;
                }
            } else {
                System.err.println("SegmentGrader未加载文件。");
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * 释放资源
     */
    private void release_file() throws IOException {
        if (buf_reader != null) {
            buf_reader.close();
            if (file_reader != null)
                file_reader.close();
        }
        buf_reader = null;
        file_reader = null;
        file_path = "";
    }

    /**
     * 返回是否考虑词性
     */
    public boolean get_consider_partOfSpeech() {
        return consider_partOfSpeech;
    }

    /**
     * 设置是否考虑词性
     * @param v 词性
     */
    public void set_consider_partOfSpeech(boolean v) {
        consider_partOfSpeech = v;
    }

    /**
     * 析构函数，释放文件资源
     * @throws Throwable 异常
     */
    @Override
    protected void finalize() throws Throwable {
        release_file();
        super.finalize();
    }
}

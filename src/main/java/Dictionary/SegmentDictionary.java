package Dictionary;

import Utils.SegmentGrader;

/**
 * 分词接口，用于{@link SegmentGrader}使用
 */
public interface SegmentDictionary {
    /**
     * 调用这个方法，传入要分词的目标字符串，返回分词结果
     * @param text 要进行分词的目标字符串
     * @return 返回的分词结果
     */
    Segment segment(String text);
}

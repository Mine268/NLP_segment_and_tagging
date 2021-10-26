package Dictionary;

import java.sql.Array;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class Segment {
    protected List<Seg_unit> segment;

    public Segment() {
        segment = new ArrayList<>();
    }

    public Segment(int i) {
        segment = new ArrayList<>(i);
    }

    public Segment(List<Seg_unit> seg) {
        segment = new ArrayList<>(seg);
    }

    /**
     * 添加一个分词单元
     * @param unit 分词单元
     * @return this
     */
    public Segment add_segment(Seg_unit unit) {
        segment.add(unit);
        return this;
    }

    /**
     * 添加一组分词单元
     * @param units 分词单元组
     * @return this
     */
    public Segment add_segments(List<Seg_unit> units) {
        segment.addAll(units);
        return this;
    }

    /**
     * 返回分词单元原始数据
     * @return 分词单元原始数据
     */
    public final List<Seg_unit> get_raw() {
        return segment;
    }

    /**
     * 获取第 {@code index} 个 {@code Seg_unit}
     * @param index 索引
     * @return 返回那个分词单元
     */
    public Seg_unit get_which(int index) {
        return segment.get(index);
    }

    /**
     * 获取词性的个数
     * @return 词性的个数
     */
    public int length() {
        return segment.size();
    }

    @Override
    public String toString() {
        return new ArrayList<>(segment).toString();
    }

    /**
     * 返回一个拷贝
     * @return 拷贝
     */
    public Segment copy() {
        return new Segment(segment);
    }
}

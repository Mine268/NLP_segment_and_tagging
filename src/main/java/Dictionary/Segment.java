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
    public final List<Seg_unit> getRaw() {
        return segment;
    }

    @Override
    public String toString() {
        return new ArrayList<>(segment).toString();
    }

}

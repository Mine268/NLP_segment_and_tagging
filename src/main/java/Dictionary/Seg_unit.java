package Dictionary;

public class Seg_unit {
    public Word word;
    public int index;

    public Seg_unit(Word v_word, int v_index) {
        word = v_word;
        index = v_index;
    }

    @Override
    public String toString() {
        return String.format("[index: %d, word: [%s]]", index, word);
    }

    /**
     * 返回一个拷贝
     * @return 拷贝
     */
    public Seg_unit copy() {
        return new Seg_unit(word.copy(), index);
    }
}

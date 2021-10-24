package Dictionary;

public class Word {
    public String text;
    public Word_info info;

    public Word(String t, Word_info i) {
        text = t;
        info = i;
    }

    public Word(String t) {
        text =t;
        info = new Word_info(0);
    }

    public Word(Word w) {
        text = w.text;
        info = new Word_info(0);
        info.type_freqList.addAll(w.info.type_freqList);
    }

    /**
     * 增添一个词性。
     * @param wi 要增添的词性
     */
    public void append_info(Word_info wi) {
        info.append_type(wi);
    }

    /**
     * 返回一个深层拷贝
     * @return 深层拷贝
     */
    public Word copy() {
        var newWord = new Word(text, new Word_info(info.type_freqList.size()));
        newWord.info.type_freqList.addAll(info.type_freqList);
        return newWord;
    }

    @Override
    public String toString() {
        return String.format("text: %s, info: %s", text, info);
    }

}

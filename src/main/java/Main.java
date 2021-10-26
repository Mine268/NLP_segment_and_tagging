import Dictionary.Seg_unit;
import Dictionary.Segment;
import Dictionary.Word;
import Utils.*;

public class Main {
    public static void main(String[] args) {
        HMMTagger hmt = new HMMTagger("data/2014_corpus.txt");
        var r = hmt.tag(new Segment()
                .add_segment(new Seg_unit(new Word("热烈"), 0))
                .add_segment(new Seg_unit(new Word("庆祝"), 0))
                .add_segment(new Seg_unit(new Word("中华人民共和国"), 0))
                .add_segment(new Seg_unit(new Word("成立"), 0))
                .add_segment(new Seg_unit(new Word("七"), 0))
                .add_segment(new Seg_unit(new Word("十"), 0))
                .add_segment(new Seg_unit(new Word("周年"), 0)));
        System.out.println(r);
    }
}

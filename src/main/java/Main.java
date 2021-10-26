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
//        hmt.print_dictionary((x, y) -> {
//            if (y.terminate) {
//                return y.word.toString();
//            } else
//                return "";
//        });

//        HMMTagger hmt = new HMMTagger();
//        hmt.insert_line("欢迎/v 朋友/n");
//        hmt.insert_line("欢迎/v 回家/v");
//        hmt.insert_line("朋友/n 回家/v");
//        hmt.insert_line("回家/n 朋友/n");
//        hmt.insert_line("欢迎/v 欢迎/v");
//        hmt.insert_line("朋友/n 欢迎/v");
//        hmt.buildup();
//
//        Segment seg = new Segment();
//        seg.add_segment(new Seg_unit(new Word("欢迎"), 0));
//        seg.add_segment(new Seg_unit(new Word("朋友"), 0));
//        seg.add_segment(new Seg_unit(new Word("回家"), 0));
//
//         var rs = hmt.tag(seg);
//        System.out.println(rs);

//        hmt.print_dictionary((x, y) -> {
//            if (y.terminate && y.word.info.get_type_sum_by_freq() > 1000) {
//                return y.word.toString();
//            } else
//                return "";
//        });
    }
}

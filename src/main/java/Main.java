import Utils.*;

public class Main {
    public static void main(String[] args) {
        HMMTagger hmt = new HMMTagger();
        hmt.insert_line("2013年12月9日/t ，/w [俄罗斯/nsf 总统/nnt]/nz 普京/nrf 下令/v 在/p 三个月/t 内/f 将/d " +
                "《/w 俄新社/nt 》/w 和/cc 《/w [俄罗斯/nsf 之/uzhi 声/qv]/nz 》/w 电台/nis 合并/v 为/p 《/w" +
                " [今日/t 俄罗斯/nsf]/nz 》/w [国际/n 通讯社/nis]/nt 。/w ");
        hmt.print_dictionary((x, y) -> {
            if (y.terminate)
                return y.word.toString();
            else
                return "";
        });
    }
}

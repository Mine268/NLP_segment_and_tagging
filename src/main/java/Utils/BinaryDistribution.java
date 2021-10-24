package Utils;

/**
 * 二项分布工具类，按照概率{@code p}输出{@code true}，否则为{@code false}.
 */
public class BinaryDistribution {
    /**
     * 按照概率为{@code p}的二项分布进行采样，1返回{@code true}，否则为{@code false}。
     * @param p 二项分布参数
     * @return 采样结果
     */
    public static boolean sample(double p) {
        return Math.random() < p;
    }
}

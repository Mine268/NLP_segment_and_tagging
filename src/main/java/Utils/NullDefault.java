package Utils;

/**
 * 工具类，用于处理常见的 null 引用转换为默认值的问题
 */
public class NullDefault {
    public static interface FetchAndReturn <T, S> {
        S consume(T val);
    }

    public static interface JustReturn <S> {
        S get();
    }

    /**
     * 该方法接受一个可能为 {@code null} 的值 {@code val}，如果它不是空引用，则调用接口 {@code FetchAndReturn} 产生相应的值，该接口的
     * 调用方式为传入这个非空的引用并返回相应的值；如果是空引用，则返回空引用下默认的值，不接受参数。注意两种情况下返回的值类型必须相同。
     * @param val 需要进行空引用处理的引用
     * @param notnull 如果非空，则会调用此接口
     * @param isnull 如果为空，则会调用此接口
     * @param <T> 进行空引用处理的引用的类型
     * @param <S> 返回处理结果的类型
     * @return 返回处理结果
     */
    public static <T, S> S nullDefault(T val, FetchAndReturn<T, S> notnull, JustReturn<S> isnull) {
        if (val == null)
            return isnull.get();
        else
            return notnull.consume(val);
    }
}

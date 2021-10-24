package Utils;

/**
 * 基础类型的pair
 */
public class PrimitivePair<T, E> implements Cloneable {
    private T left;
    private E right;

    public PrimitivePair(T t, E e) {
        left = t;
        right = e;
    }

    public void setLeft(T t) {
        left = t;
    }

    public void setRight(E e) {
        right = e;
    }

    public T getLeft() {
        return left;
    }

    public E getRight() {
        return right;
    }

    @Override
    public PrimitivePair<T, E> clone() throws CloneNotSupportedException {
        PrimitivePair<T, E> clone = (PrimitivePair<T, E>) super.clone();
        clone.left = left;
        clone.right = right;
        return clone;
    }
}

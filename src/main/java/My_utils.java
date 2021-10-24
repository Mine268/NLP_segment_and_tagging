import Utils.ACTrie;

import java.util.stream.Collectors;

public class My_utils {
    public static class my_stringer1 implements ACTrie.ACTrie_node.ACTrie_node_stringer {
        @Override
        public String fetchString(String key, ACTrie.ACTrie_node node) {
            return String.format(
                    "[%s] from edge: %s, fail: %s, terminate: %s, next: %s, fail_inv: %s",
                    node.toString(),
                    key,
                    node.fail,
                    node.terminate,
                    node.next.values().stream()
                            .map(Object::toString)
                            .collect(Collectors.toList()),
                    node.fail_inv.stream()
                            .map(Object::toString)
                            .collect(Collectors.toList())
            );
        }
    }
}

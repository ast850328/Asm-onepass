public class RefNode {
    String pos;
    boolean index;
    RefNode next;
    int line;
    public RefNode(String pos, boolean index, int line) {
        this.pos = pos;
        this.index = index;
        this.line = line;
    }
}
public class SNode {
    String label;
    String addr;
    boolean flag;
    SNode next;
    RefNode ref;
    public SNode (String label, String addr) {
        this.label = label;
        this.addr = addr;
        flag = true;
    }
    public SNode (String label) {
        this.label = label;
        flag = false;
    }
}
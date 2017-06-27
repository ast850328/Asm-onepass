import java.util.Scanner;

public class SymbolTable {
    SNode[] data;
    int n;
    public SymbolTable(int x) {
        n = x;
        data = new SNode[n];
    }
    public boolean insert(String label, String addr) {
        int slot;
        if (label.hashCode() < 0)
            slot = -label.hashCode() % n;
        else
            slot = label.hashCode() % n;
        SNode tmp = data[slot];
        while (tmp != null) {
            if (tmp.label.equals(label))
                break;
            tmp = tmp.next;
        }
        // duplicate data
        if (tmp != null) {
            // System.out.println("The label has already been used!");
            if (tmp.flag)
                return false;
            else {
                tmp.addr = addr;
                return true;
            }
        }
        // push data
        tmp = new SNode(label, addr);
        tmp.next = data[slot];
        data[slot] = tmp;

        return true;
    }
    public String search(String label) {
        int slot;
        if (label.hashCode() < 0)
            slot = -label.hashCode() % n;
        else
            slot = label.hashCode() % n;
        SNode tmp = data[slot];
        while (tmp != null) {
            if (tmp.label.equals(label)) {
                return tmp.addr;
            }
            tmp = tmp.next;
        }
        return null;
    }
    public void print() {
        SNode tmp;
        int index = 1;
        for (int i = 0; i < n; i++) {
            tmp = data[i];
            while (tmp != null) {
                System.out.println(index + " " + i + " " + tmp.label + " " + tmp.addr);
                tmp = tmp.next;
                index++;
            }
        }
    }
    public void printRef() {
        SNode tmp;
        for (int i = 0; i < n; i++) {
            tmp = data[i];
            while (tmp != null) {
                System.out.println(tmp.label + " " + tmp.addr);
                RefNode ref = tmp.ref;
                while (ref != null){
                    System.out.println(ref.pos);
                    ref = ref.next;
                }
                tmp = tmp.next;
            }
        }
    }
    public void insertRef(String label, String pos, boolean index, int line) {
        int slot;
        if (label.hashCode() < 0)
            slot = -label.hashCode() % n;
        else
            slot = label.hashCode() % n;
        SNode tmp = data[slot];
        while (tmp != null) {
            if (tmp.label.equals(label))
                break;
            tmp = tmp.next;
        }
        if (tmp != null) {
            RefNode ref = tmp.ref;
            while (ref != null)
                ref = ref.next;
            ref = new RefNode(pos, index, line);
            ref.next = tmp.ref;
            tmp.ref = ref;
            return ;
        }
        tmp = new SNode(label, null);
        tmp.flag = false;
        tmp.ref = new RefNode(pos, index, line);
        tmp.next = data[slot];
        data[slot] = tmp;
    }
    public static void main (String[] argc) {
        SymbolTable sym = new SymbolTable(13);
        sym.insertRef("QQ", "1", true, 1);
        sym.insertRef("QQ", "2", false,2);
        sym.printRef();
    }
}
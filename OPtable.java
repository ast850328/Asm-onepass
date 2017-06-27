import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Scanner;

public class OPtable {
    OPNode[] data;
    int n;
    public OPtable(int x) {
        n = x;
        data = new OPNode[n];
    }
    public void push(String mm, String code) {
        int slot;
        if (mm.hashCode() < 0)
            slot = -mm.hashCode() % n;
        else
            slot = mm.hashCode() % n;
        OPNode tmp;
        for (tmp = data[slot]; tmp != null; tmp = tmp.next)
            if (tmp.mm.equals(mm))
                break;
        // duplicate data
        if (tmp != null) {
            tmp.code = code;
            return;
        }
        // push data
        tmp = new OPNode(mm, code);
        tmp.next = data[slot];
        data[slot] = tmp;
    }
    public void print() {
        OPNode tmp;
        int index = 1;
        for (int i = 0; i < n; i++) {
            tmp = data[i];
            while (tmp != null) {
                System.out.println(index + " " + i + " " + tmp.mm + " " + tmp.code);
                tmp = tmp.next;
                index++;
            }
        }
    }
    public String search(String mm) {
        int slot;
        if (mm.hashCode() < 0)
            slot = -mm.hashCode() % n;
        else
            slot = mm.hashCode() % n;
        OPNode tmp = data[slot];
        while (tmp != null) {
            if (tmp.mm.equals(mm)) {
                // System.out.println(mm + " " + tmp.code);
                return tmp.code;
            }
            tmp = tmp.next;
        }
        // System.out.println("Not found or wrong input!");
        return null;
    }
    public static void main(String[] argc) throws IOException {
        FileReader fr = new FileReader("opCode.txt");
        BufferedReader br = new BufferedReader(fr);
        OPtable t = new OPtable(13);
        while (br.ready()) {
            String line = br.readLine();
            String[] data = line.split(" ");
            t.push(data[0], data[1]);
        }
        
        t.print();
        System.out.println("Start searching:");
        
        Scanner input = new Scanner(System.in);
        String mm;
        while(input.hasNext()) {
            mm = input.next();
            if (t.search(mm) == null)
                System.out.println("Not found");
            else
                System.out.println(t.search(mm));
        }
    }
}
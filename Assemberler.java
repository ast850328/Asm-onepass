import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileWriter;

// for one pass SIC
public class Assemberler {
    static String loctr;
    static String objectCode = "";
    static String[] list = new String[]{"START", "END", "WORD", "BYTE", "RESW", "RESB"};
    // 16 進位相加
    public static String add(String hex1, String hex2) { 
        return "0x" + Integer.toHexString(Integer.decode(hex1) + Integer.decode(hex2)); 
    }
    // 16 進位相減
    public static String minus(String hex1, String hex2) { 
        return "0x" + Integer.toHexString(Integer.decode(hex1) - Integer.decode(hex2)); 
    }
    // 10 進位 轉 16 進位
    public static String base(String data) {
        String result = "";
        int tmp = Integer.parseInt(data);
        int number = 0;
        while (tmp != 0) {
            number = tmp % 16;
            if (number >= 10)
                result = (char)(number + 87) + result;
            else
                result = tmp % 16 + result;
            tmp = tmp / 16;
        }
        return result;
    }
    // 把 16 進位 0x 拿掉
    public static String normalize(String data) {
        String result = data.substring(2);
        while (result.length() != 4) {
            result = "0" + result;
        }
        return result;
    }
    // 寫入 tRecord
    public static void tWrite(int tNum,String tStart,String tRecord) {
        String number = base(Integer.toString(tNum));
        while (number.length() != 2) {
            number = "0" + number;
        }
        objectCode += "T^00" + normalize(tStart) + "^" + number + tRecord + "\n";     
    }
    // 判斷是不是虛指令 START, END, WORD, BYTE, RESW, RESB
    public static int searchCmd(String data) {
        int i;
        for (i = 0; i < list.length; i++) {
            if (data.equals(list[i]))
                return i;
        }
        return -1;
    }
    // 檢查 label 的 flag
    public static SNode flagCheck(String label, SymbolTable s) {
        int slot;
        if (label.hashCode() < 0)
            slot = -label.hashCode() % s.n;
        else
            slot = label.hashCode() % s.n;
        SNode tmp = s.data[slot];
        while (tmp != null) {
            if (tmp.label.equals(label)) {
                if (tmp.flag == true)
                    return null;
                else 
                    return tmp;
            }
            tmp = tmp.next;
        }
        return null;
    }
    // 如果 flag 是 false 要補之前的 forward reference
    public static void flagWrite(SNode tmp) {
        tmp.flag = true;
        while (tmp.ref != null) {
            if (tmp.ref.index == true)
                objectCode += "T^00" + normalize(tmp.ref.pos) + "^02^" + add(tmp.addr, "0x8000").substring(2)+"\n";
            else
                objectCode += "T^00" + normalize(tmp.ref.pos) + "^02^" + tmp.addr.substring(2)+"\n";
            tmp.ref = tmp.ref.next;
        }
    }
    // 檢查所有的 label 之 flag 還有沒有沒設成 true 的
    public static boolean flagErrCheck(SymbolTable s) {
        SNode tmp;
        int index = 1;
        boolean result = false;
        for (int i = 0; i < s.n; i++) {
            tmp = s.data[i];
            while (tmp != null) {
                if (tmp.flag == false) {
                    result = true;
                    System.out.print("undefined label " + "\'" + tmp.label + "\', Line: ");
                    RefNode ref = tmp.ref;
                    while (ref != null) {
                        System.out.print(ref.line + ", ");
                        ref = ref.next;
                    }
                    System.out.println();
                }
                tmp = tmp.next;
                index++;
            }
        }
        return result;
    }
    public static void main(String[] args) throws IOException {
            FileWriter fw = new FileWriter("ObjectFile.txt");
            FileReader fr;
            BufferedReader br;
            // OPTable
            fr = new FileReader("opCode.txt");
            br = new BufferedReader(fr);
            OPtable op = new OPtable(13);
            while (br.ready()) {
                String line = br.readLine();
                String[] data = line.split(" ");
                op.push(data[0], data[1]);
            }
            // SymbolTable
            SymbolTable s = new SymbolTable(13);
            // test.txt reading
            fr = new FileReader("test.txt");
            br = new BufferedReader(fr);  
            int indexLine = 1; // 行號
            String programName = ""; // 程式名字
            String start = ""; // 起始位址
            String end = ""; // 程式進入位址
            int tNum = 0; // tRecord 放的數量
            String tStart = ""; // tRecord 從哪開始
            String tRecord = ""; // tRecord 先清空
            boolean index = false; // 判斷是不是 inde
            String operand = "";  // operand
            int errNum = 0; // error 數量
            while (br.ready()) {
                try {
                    String line = br.readLine();
                    String[] data;
                    // 拿掉註解
                    int p;
                    if ((p = line.indexOf('\'',line.indexOf('\'')+1)) >= 0) {
                        String lineTmp = line.substring(p+1);
                        if (lineTmp.indexOf('.') >= 0)
                            line= line.substring(0, p+1+lineTmp.indexOf('.'));
                    } else {
                        if (line.indexOf(".") >= 0)
                            line = line.substring(0, line.indexOf("."));
                    }
                    // 空行跳過
                    if (line.trim().equals("")) {
                    } else if(line.getBytes().length != line.length()) {
                        throw new ChException(Integer.toString(indexLine));
                    }else {
                        // 最多切三個
                        data = line.trim().split("\\s+", 3);
                        // no label
                        if (op.search(data[0]) != null) {
                            // tNum + 3 判斷
                            if (tNum + 3 > 30) {
                                tWrite(tNum, tStart, tRecord);
                                tNum = 0;
                                tRecord = "";
                                tStart = loctr;
                            }
                            // 如果是 RSUB
                            if (data[0].equals("RSUB")){
                                if (data.length > 1)
                                    throw new RSUBException(Integer.toString(indexLine));
                                else
                                    tRecord += "^" + op.search(data[0]) + "0000";
                            } else {
                            // 如果是其他指令
                                // 判斷有沒有 operand
                                if (data.length == 1)
                                    throw new NoOperandException(Integer.toString(indexLine));
                                // 把後面的 operand 合併
                                if (data.length > 2) {
                                    for (int i = 2; i < data.length; i++)
                                        data[1] += " " + data[i];
                                }
                                // 判斷是不是 index form
                                int pos;
                                if ((pos = data[1].indexOf(",")) >= 0){
                                    operand = data[1].substring(0, pos).trim();
                                    if (!data[1].substring(pos+1).trim().equals("X"))
                                        throw new IndexException(Integer.toString(indexLine));
                                    index = true;
                                } else {
                                    String[] test = data[1].trim().split("\\s+");
                                    if (test.length > 1) {
                                        throw new OperandException(Integer.toString(indexLine));
                                    }
                                    operand = data[1].trim();
                                }
                                // 判斷 operand 是不是 虛指令 或是 mnemonic 
                                if (searchCmd(operand) != -1 || op.search(operand) != null)
                                    throw new OperandException(Integer.toString(indexLine));
                                // 如果在 Symbol table 沒找到，insert and set flag false
                                if (s.search(operand) == null) {
                                    s.insertRef(operand, add(loctr,"0x1"), index, indexLine);
                                    tRecord += "^" + op.search(data[0]) + "0000";
                                } else {
                                    if (index)
                                        tRecord += "^" + op.search(data[0]) + normalize(add(s.search(operand),"0x8000"));
                                    else
                                        tRecord += "^" + op.search(data[0]) + normalize(s.search(operand));
                                }
                            }
                            index = false;
                            tNum += 3;
                            loctr = add(loctr, "0x3");
                        }
                        // has label 
                        else {
                            // 不能只有label
                            if (data.length == 1)
                                throw new CmdException(Integer.toString(indexLine));
                            // 如果 label 是 END
                            if (searchCmd(data[0]) == 1){
                                for (int i = 2; i < data.length; i++)
                                    data[1] += data[i];
                                if (searchCmd(data[1]) != -1 || op.search(data[1]) != null)
                                    throw new OperandException(Integer.toString(indexLine));
                                if (s.search(data[1]) == null)
                                    throw new LabelNotFoundException(Integer.toString(indexLine));
                                end = s.search(data[1]);
                                break;
                            // 如果是其他 虛指令
                            } else if (searchCmd(data[0]) != -1) {
                                throw new WrongLabelException(Integer.toString(indexLine));
                            } else {
                                // 如果 command 是 END
                                if (data[1].equals("END"))
                                    throw new EndException(Integer.toString(indexLine));
                                // 如果 command 是 START
                                else if (data[1].equals("START")) {
                                    if (data.length != 3)
                                        throw new StartException(Integer.toString(indexLine));
                                    String[] test = data[2].trim().split("\\s+");
                                    for (int i = 0; i < data[2].length(); i++)
                                        if (data[2].charAt(i) < '0' || data[2].charAt(i) > '9')
                                            throw new StartException(Integer.toString(indexLine));    
                                    if (test.length > 1)
                                        throw new StartException(Integer.toString(indexLine));
                                    programName = data[0];
                                    start = "0x" + data[2];
                                    loctr = start;
                                    tStart = loctr;
                                    indexLine++;
                                    continue;
                                // 如果是其他 Command
                                } else {
                                    // 把 label 放進去
                                    if (!s.insert(data[0], loctr)) {
                                        throw new LabelException(Integer.toString(indexLine));
                                    }
                                    // 檢查 label 的 flag
                                    SNode tmpSNode = flagCheck(data[0], s);
                                    if (tmpSNode != null) {
                                        if (tNum > 0) {
                                            tWrite(tNum, tStart, tRecord);
                                            tNum = 0;
                                            tRecord = "";
                                            tStart = loctr;
                                        }
                                        flagWrite(tmpSNode);
                                    }
                                    // Command 的是 END
                                    if (searchCmd(data[1]) != -1) {
                                        if (data.length < 3)
                                            throw new NoOperandException(Integer.toString(indexLine));
                                        if (data[1].equals("RESW")){
                                            String bytes = Integer.toString(Integer.parseInt(data[2]) * 3);
                                            if (tNum > 0) {
                                                tWrite(tNum, tStart, tRecord);
                                                tNum = 0;
                                                tRecord = "";
                                            }
                                            loctr = add(loctr, "0x" + base(bytes));
                                            tStart = loctr;                
                                        } else if (data[1].equals("RESB")) {
                                            if (tNum > 0) {
                                                tWrite(tNum, tStart, tRecord);
                                                tNum = 0;
                                                tRecord = "";
                                            }
                                            loctr = add(loctr, "0x" + base(data[2]));
                                            tStart = loctr;                
                                        } else if (data[1].equals("WORD")) {
                                            if (tNum + 3 > 30) {
                                                tWrite(tNum, tStart, tRecord);
                                                tNum = 0;
                                                tRecord = "";
                                                tStart = loctr;
                                            }
                                            loctr = add(loctr, "0x3");
                                            tNum += 3;
                                            tRecord += "^" + "00" + normalize("0x" + base(data[2]));                
                                        } else if (data[1].equals("BYTE")) {
                                            if (data[2].indexOf('\'') < 0 || data[2].indexOf('\'', 2) < 0 || !data[2].substring(data[2].indexOf('\'', 2)+1).trim().equals(""))
                                                throw new OperandException(Integer.toString(indexLine));
                                            if (data[2].substring(0, data[2].indexOf('\'')).trim().equals("X")) {
                                                String test = data[2].substring(data[2].indexOf('\'')+1, data[2].indexOf('\'', 2));
                                                for (int i = 0; i < test.length(); i++) {
                                                    if (test.charAt(i) == ' ' || !((test.charAt(i) >= '0' && test.charAt(i) <= '9') || ((test.charAt(i) >= 'A') && (test.charAt(i) <= 'F')))){
                                                        throw new XException(Integer.toString(indexLine));
                                                    }
                                                }
                                                if (test.length() % 2 != 0)
                                                    throw new XException(Integer.toString(indexLine));
                                                if (tNum + (test.length() / 2) > 30) {
                                                    tWrite(tNum, tStart, tRecord);
                                                    tNum = 0;
                                                    tRecord = "";
                                                    tStart = loctr;
                                                }
                                                loctr = add(loctr, "0x" + test.length() / 2);
                                                tNum += (test.length() / 2);
                                                tRecord += "^" + test;
                                            } else if (data[2].substring(0, data[2].indexOf('\'')).trim().equals("C")) {
                                                String tmp = data[2].substring(data[2].indexOf('\'')+1, data[2].indexOf('\'', 2));
                                                if (tNum + tmp.length() > 30) {
                                                    tWrite(tNum, tStart, tRecord);
                                                    tNum = 0;
                                                    tRecord = "";
                                                    tStart = loctr;
                                                }
                                                loctr = add(loctr, "0x"+tmp.length());
                                                tNum += tmp.length();
                                                String tmpString = "";
                                                for (int i = 0; i < tmp.length(); i++)
                                                    tmpString += Integer.toHexString((int)tmp.charAt(i));
                                                tRecord += "^" + tmpString;
                                            } else {
                                                throw new OperandException(Integer.toString(indexLine));
                                            }
                                        }
                                    // Command 不是虛指令
                                    } else {
                                        if (op.search(data[1]) == null)
                                            throw new OPException(Integer.toString(indexLine));
                                        if (tNum + 3 > 30) {
                                            tWrite(tNum, tStart, tRecord);
                                            tNum = 0;
                                            tRecord = "";
                                            tStart = loctr;
                                        }
                                        if (data[1].equals("RSUB")){
                                            if (data.length > 2)
                                                throw new RSUBException(Integer.toString(indexLine));
                                            else
                                                tRecord += "^" + op.search(data[1]) + "0000";
                                        } else {
                                            if (data.length == 2)
                                                throw new NoOperandException(Integer.toString(indexLine));
                                            int pos;
                                            if ((pos = data[2].indexOf(",")) >= 0){
                                                operand = data[2].substring(0, pos).trim();
                                                if (!data[2].substring(pos+1).trim().equals("X"))
                                                    throw new IndexException(Integer.toString(indexLine));
                                                index = true;
                                            } else {
                                                String[] test = data[2].trim().split("\\s+");
                                                if (test.length > 1) {
                                                    throw new OperandException(Integer.toString(indexLine));
                                                }
                                                operand = data[2].trim();
                                            }
                                            if (searchCmd(operand) != -1 || op.search(operand) != null)
                                                throw new OperandException(Integer.toString(indexLine));
                                            if (s.search(operand) == null) {
                                                s.insertRef(operand, add(loctr,"0x1"), index, indexLine);
                                                tRecord += "^" + op.search(data[1]) + "0000";
                                            } else {
                                                if (index)
                                                    tRecord += "^" + op.search(data[1]) + normalize(add(s.search(operand),"0x8000"));
                                                else{
                                                    tRecord += "^" + op.search(data[1]) + normalize(s.search(operand));    
                                                }
                                            }
                                        }
                                        index = false;
                                        tNum += 3;
                                        loctr = add(loctr, "0x3");
                                    }
                                }
                            }
                        }
                    }
                } catch (RSUBException err1) {
                    errNum++;
                    System.out.println("Line: " + err1.getMessage() + ", RSUB doesn't need operand.");
                } catch (LabelException err2) {
                    errNum++;
                    System.out.println("Line: " + err2.getMessage() + ", duplicate label.");
                } catch (WrongLabelException err3) {
                    errNum++;
                    System.out.println("Line: " + err3.getMessage() + ", wrong label.");
                } catch (StartException err4) {
                    System.out.println("Line: " + err4.getMessage() + ", wrong start.");
                    return ;
                } catch (OperandException err5) {
                    errNum++;
                    System.out.println("Line: " + err5.getMessage() + ", can't find operand or wrong operand.");
                }catch (NoOperandException err6) {
                    errNum++;
                    System.out.println("Line: " + err6.getMessage() + ", need Operand.");
                } catch (IndexException err7) {
                    errNum++;
                    System.out.println("Line: " + err7.getMessage() + ", wrong index form.");
                } catch (LabelNotFoundException err8) {
                    errNum++;
                    System.out.println("Line: " + err8.getMessage() + ", label not found.");
                } catch (CmdException err9) {
                    errNum++;
                    System.out.println("Line: " + err9.getMessage() + ", need mnemonic.");
                } catch (EndException err10) {
                    errNum++;
                    System.out.println("Line: " + err10.getMessage() + ", don't need label.");
                } catch (XException err11) {
                    errNum++;
                    System.out.println("Line: " + err11.getMessage() + ", BYTE X error.");
                } catch (OPException err12) {
                    errNum++;
                    System.out.println("Line: " + err12.getMessage() + ", Wrong command.");
                } catch (ChException err13) {
                    errNum++;
                    System.out.println("Line: " + err13.getMessage() + ", No Chinese.");
                } catch (Exception err) {
                    errNum++;
                    System.out.println("Line: " + indexLine + ", operand should be number form.");
                }
                indexLine++;
            }
            if (errNum > 0)
                return;
            if (tNum > 0)
                tWrite(tNum, tStart, tRecord);
            if (flagErrCheck(s))
                return;
            String length = minus(loctr, start);
            String hRecord = "H^" + programName + "   00" + normalize(start) + "^00" + normalize(length) + "\n";
            objectCode = hRecord + objectCode;
            objectCode += "E^00" + normalize(end);
            System.out.println(objectCode.toUpperCase());
            fw.write(objectCode.toUpperCase());
            fw.close();
            fr.close();
    } 
}
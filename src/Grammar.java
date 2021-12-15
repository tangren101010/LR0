import java.util.*;

public class Grammar {
    private static final char Njump = 'ε';
    private static final char Dian = '･';
    Set<String> VN;//非终结符
    Set<String> VT;//终结符集
    Set<Production> P;//规则集
    String S;//开始符
    Set<IE> IESet;//活前缀项目集间的边集,项目集-箭弧-项目集
    ArrayList<Set<Production>> IList;//项目集的数组，包含该项目集的所有项目
    ArrayList<Production> PList;//规则集的数组，文法G‘中所有的产生式
    String[][] LRTable;//存放LR0分析表的二维数组
    ArrayList<String> tableHead;
    Boolean isLRO = true;

    public Grammar(Set<String> VN, Set<String> VT, Set<Production> p, String s) {
        this.VN = VN;
        this.VT = VT;
        P = p;
        S = s;
    }

    public Grammar(String[] ip, String S) {
        this.S = "S'";
        VN = new HashSet<>();
        VT = new HashSet<>();
        P = new HashSet<>();//规则集，文法G‘中所有的产生式
        VN.add(S);
        P.add(new Production(this.S + "->" + S));
        for (int i = 0; i < ip.length; i++) {
            Production p = new Production(ip[i]);
            VN.add(p.getLeft());//添加所有的非终结符
            if (p.isSimple()) {
                P.add(p);
                String pr = p.getRight();
                for (int j = 0; j < pr.length(); j++) {
                    VT.add(String.valueOf(pr.charAt(j)));
                }
            } else {
                for (Production sp : p.toSimple()) {
                    P.add(sp);
                    String spr = sp.getRight();
                    for (int j = 0; j < spr.length(); j++) {
                        VT.add(String.valueOf(spr.charAt(j)));//先将右部所有的字符加入到终结符集合中，在后续会进行删除
                    }
                }
            }
        }
        VT.removeAll(VN);//删除终结符中的非终结符
        VT.remove(String.valueOf(Njump));//删除空

        Set<Production> I0 = new HashSet<>();//活前缀项目集的开始集
        I0.add(new Production(this.S + "->" + Dian + S));//I0: S'->E
        calculationCLOSURE(I0);
        IList = new ArrayList<>();//项目集
        IList.add(I0);//将I0添加至项目集
        calculationDFA();
        //如果创建LR0分析表失败，则输出该文法不是LR(0)文法
        if (!(isLRO = createLRTable())) {
            System.out.println("NO LR(0)!");
        }

    }

    //对项目集I进行闭包运算
    private void calculationCLOSURE(Set<Production> I) {
        Set<String> nV = new HashSet<>();//点后面的非终结符
        int ISize;
        do {
            ISize = I.size();//项目集
            for (Production i : I) {
                String iRight = i.right;
                int Di = iRight.indexOf(Dian);
                if (Di + 1 < iRight.length()) {
                    String inV = iRight.substring(Di + 1, Di + 2);
                    if (VN.contains(inV)) {
                        nV.add(inV);
                    }
                }
            }
            for (Production ip : P) {
                if (nV.contains(ip.left)) {
                    I.add(ip.insertDian());//加点
                }
            }
        } while (ISize != I.size());
    }

    //求活前缀DFA,得到边集、项目集
    private void calculationDFA() {
        IESet = new HashSet<>();
        Queue<Set<Production>> queue = new LinkedList<>();
        queue.add(IList.get(0));//将项目集的第一个元素加入队列
        Set<Production> nI;
        Map<String, Set<Production>> nIMap;//nIMap的key，nIMap是一个字典,存放键值对

        while (!queue.isEmpty()) {      //对一个项目集里面的每个项目都进行活前缀后移一位
            Set<Production> iI = queue.poll();
            nIMap = new HashMap<>();
            //活前缀后移一位后的不同结果
            for (Production i : iI) {
                String iRight = i.right;
                int Di = iRight.indexOf(Dian);//找出点的位置
                if (Di + 1 < iRight.length()) {   //表示点后移一位，项目中点的后面还有字符的情况
                    String iV = iRight.substring(Di + 1, Di + 2);//iV就是点后面的字符
                    nI = nIMap.get(iV);//设置键值对
                    if (nI == null) { //如果nI为空
                        nI = new HashSet<>();
                        nI.add(i.moveDian());//移点
                        nIMap.put(iV, nI);//将移点后的项目加入
                    } else {
                        nI.add(i.moveDian());//
                    }
                }
            }
            //对活前缀后移一位，后面还有字符的情况，进行闭包运算
            int iList = IList.indexOf(iI);
            for (String v : nIMap.keySet()) {
                nI = nIMap.get(v);
                calculationCLOSURE(nI);//闭包运算

                int jList = IList.indexOf(nI);//检查进行闭包运算得到的项目集是否已经存在，如果不存在将其加入
                if (jList == -1) {
                    queue.add(nI);
                    IList.add(nI);
                    jList = IList.size() - 1;
                }
                IESet.add(new IE(iList, v, jList));
            }
            nIMap.clear();
        }
    }

    /*
    实现LR0表的输出
     */
    public void out() {
        //输出非终结符
        System.out.println("VN:");
        for (Object iVN : VN) {
            System.out.println(iVN);
        }
        //输出终结符
        System.out.println("VT:");
        for (Object iVT : VT) {
            System.out.println(iVT);
        }
        //输出该文法
        System.out.println("P:");
        for (Object ip : PList) {
            System.out.println(ip.toString());
        }
        System.out.println("S:");
        System.out.println(S);
        //输出识别活前缀的DFA
        System.out.println("IESet:");
        System.out.println(IESet);
        //输出LR0分析表
        System.out.println("LR0Table:");
        System.out.printf("%30s", "LR0");
        for (String i : tableHead) {
            System.out.printf("%10s", i);
        }
        System.out.print("\n");
        for (int i = 0; i < LRTable.length; i++) {
            System.out.printf("%30s", "S" + i + IList.get(i));
            for (int j = 0; j < LRTable[0].length; j++) {
                if (LRTable[i][j] != null)
                    System.out.printf("%10s", LRTable[i][j]);
                else
                    System.out.printf("%10s", "");
            }
            System.out.print("\n");
        }
    }

    //构造LR0分析表
    public boolean createLRTable() {
        PList = new ArrayList<>();
        PList.addAll(P);

        tableHead = new ArrayList<>();
        tableHead.addAll(VT);
        tableHead.add("#");
        tableHead.addAll(VN);
        tableHead.remove(S);
//        ArrayList<Set<Production>> IList;//项目集的数组
        LRTable = new String[IList.size()][tableHead.size()];
        //构造GOTO表
        int go[][] = new int[IList.size()][tableHead.size()];
        for (IE ie : IESet) {
            int ivalue = tableHead.indexOf(ie.getValue());
            int k = ie.getOrgin();
            if (go[k][ivalue] == 0)
                go[k][ivalue] = ie.getAim();
            else
                return false;
        }
        //填Action表
        for (int k = 0; k < IList.size(); k++) {
            for (Production ip : IList.get(k)) {
                String right = ip.getRight();
                int iD = right.indexOf(Dian);
                if (iD < right.length() - 1) { //A->α･aβ GO(Ik,a)=Ij
                    String a = right.substring(iD + 1, iD + 2);
                    if (VT.contains(a)) {//a为终结符
                        int ia = tableHead.indexOf(a);
                        if (LRTable[k][ia] == null)
                            LRTable[k][ia] = "S" + go[k][ia];
                        else
                            return false;
                    }
                } else {//A->α･
                    if (ip.getLeft().equals(S)) {
                        if (LRTable[k][VT.size()] == null)
                            LRTable[k][VT.size()] = "acc";
                        else
                            return false;
                    } else {
                        for (int ia = 0; ia < VT.size() + 1; ia++) {
                            if (ip.getLeft().equals("A")) {
                                int b = 0;
                            }
                            if (LRTable[k][ia] == null)
                                LRTable[k][ia] = "r" + PList.indexOf(ip.deleteDian());
                            else
                                return false;
                        }
                    }
                }
            }
        }


        //合并表
        for (int j = VT.size(); j < tableHead.size(); j++) {
            for (int k = 0; k < IList.size(); k++) {
                if (go[k][j] != 0) {
                    if (LRTable[k][j] == null)
                        LRTable[k][j] = "" + go[k][j];
                    else
                        return false;
                }
            }
        }
        return true;
    }

    //判断句子是否符合文法，LR0分析器
    public boolean contains(String st) throws Exception {
        if (isLRO) {
            st += "#";
            Stack<Integer> stateStack = new Stack<>();
            Stack<String> signStack = new Stack<>();
            stateStack.push(0);
            signStack.push("#");
            Production p;
            int VTL = VT.size();
            int bz = 0;//步骤数
            for (int i = 0; i < st.length(); i++) {
                if (true) {//显示分析过程
                    System.out.println(++bz);
                    System.out.print("状态栈：");
                    System.out.println(stateStack);
                    System.out.print("符号栈：");
                    System.out.println(signStack);
                    System.out.print("输入串：");
                    System.out.println(st.substring(i));
                }
                String a = st.substring(i, i + 1);
                int ai = tableHead.indexOf(a);
                String ag = LRTable[stateStack.peek()][ai];
                if (ag == null) return false;
                else if (ag.equals("acc")) {
                    return true;
                }
                if (ai < VT.size() + 1) {//action
                    int nub = Integer.valueOf(ag.substring(1));
                    if (ag.charAt(0) == 'S') {
                        stateStack.push(nub);
                        signStack.push(a);
                    } else {//r
                        p = PList.get(nub);
                        int k = p.getRight().length();
                        if (!p.getRight().equals(String.valueOf(Njump))) {//排除归约为 A->ε
                            while (k-- > 0) {
                                stateStack.pop();
                                signStack.pop();
                            }
                        }
                        //goto
                        String go = LRTable[stateStack.peek()][tableHead.indexOf(p.getLeft())];
                        if (go == null) return false;
                        stateStack.push(Integer.valueOf(go));
                        signStack.push(p.getLeft());
                        i--;
                    }
                }
            }
            return false;
        }
        throw new Exception("无法判断：该文法不是LR(0)文法！");

    }
}

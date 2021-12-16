public class Main {
    private static final char Njump = 'ε';
    public static void main(String[] args) {
        String S="E";//开始符
//        String P[]={"E->aA|bB","A->cA|d","B->cB|d"};//规则集
        String P[]={"E->aA|bBC","C->"+Njump,"A->cA|d","B->cB|d"};//规则集
        Grammar G=new Grammar(P,S);

        G.out();//输出LRO分析表
//        try {
//            System.out.print(G.contains("bccd"));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }


    }
}

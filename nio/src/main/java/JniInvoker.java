public class JniInvoker {

    public native static String getStringFromCPP();


    public static void main(String[] args) {
        System.out.println(getStringFromCPP());
    }
}

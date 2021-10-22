package com.leo.algorithm.linked;

/**
 * 链表反转-倒序-正序输出
 * @author leo
 */
public class ReverseLinked {


    static class LinkedItem {
        LinkedItem next;
        int value;

        public LinkedItem(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public LinkedItem getNext() {
            return next;
        }

        public void setNext(LinkedItem next) {
            this.next = next;
        }
    }

    public static void main(String[] args) {
        LinkedItem item = new LinkedItem(0);
        LinkedItem item1 = new LinkedItem(1); item.setNext(item1);
        LinkedItem item2 = new LinkedItem(2); item1.setNext(item2);
        LinkedItem item3 = new LinkedItem(3); item2.setNext(item3);
        System.out.println("-------------正序-----------------");
        printOrdered(item);
        System.out.println("--------------倒序----------------");
        print(item);
        // 思路： 左 --> 右
        LinkedItem left = item;
        LinkedItem right = left.next;
        left.next = null;
        // exit condition: 右边为空，则返回左
        LinkedItem temp = reverse(left, right);
        // print
        System.out.println("--------------反转正序----------------");
        printOrdered(temp);
    }



    private static void print(LinkedItem item){
        if(item.next != null) {
            print(item.next);
        }
        System.out.println(item.getValue());
        return;
    }



    private static void printOrdered(LinkedItem item){
        if(item != null) {
            System.out.println(item.getValue());
            printOrdered(item.next);
        }
        return;
    }

    public static LinkedItem reverse(LinkedItem left, LinkedItem right) {
        if(right == null) {
            return left;
        }
        LinkedItem next = right.next;
        right.next = left;
        left = right;
        right = next;
        return reverse(left, right);
    }
}

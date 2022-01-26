package com.leo.algorithm.dynamicPlan;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Stack;

public class Floor {

    static Stack<Integer> stack = new Stack<>();

    static int step[] = new int[]{2, 3, 5};

    public static void main(String[] args) {
        dynamic(6);
        System.out.println("==================================");
        dynamic(7);
        System.out.println("==================================");
        dynamic(9);
        System.out.println("==================================");
        dynamic(11);
        System.out.println("================================");


        int[][] inDate = new int[][] {{32}, {83, 68}, {40, 37, 47}, {5, 4, 67, 22}, {79, 69, 78, 29, 63}, {0, 71, 51, 82, 91, 64}};
        System.out.println(recursion(inDate));
    }

    public static int recursion(int[][] a) {
        return recursion(a, 0, 0);
    }



    public static int recursion(int[][] a, int i , int z) {

        if(i == a.length - 1) {
            return a[i][z];
        }


        int x = recursion(a, i+1, z);

        // 横向扩展不一定到底
        int y = recursion(a, i+1, z + 1);
        return Math.max(x, y) + a[i][z];
    }



    public static int recursion(int n) {

        if (n < 0) {
            return 0;
        }

        if (n == 0) {
            return 1;
        }

        return recursion(n - 5) + recursion(n - 3) + recursion(n - 2);
    }


    /**
     * 后反推
     *
     * @param n
     * @return
     */
    public static void dynamic(int n) {
        // min step
        if (n < 0) {
            return;
        }

        if (n - 2 == 0 || n - 3 == 0 || n - 5 == 0) {
            stack.push(n);
            int length = stack.size();
            Enumeration<Integer> elements = stack.elements();
            while (elements.hasMoreElements()) {
                Integer res = elements.nextElement();
                System.out.print(res + " <");
            }
            System.out.println("-------------------------------" + length);
            stack.pop();
        }

        for (int i : step) {
            stack.push(i);
            dynamic(n - i);
            stack.pop();
        }
    }
}

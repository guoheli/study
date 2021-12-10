package com.leo.algorithm.tree;

import lombok.Data;
import org.junit.Before;
import org.junit.Test;

import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.ArrayBlockingQueue;

@Data
public class TreeNode {
    int value;
    TreeNode left;
    TreeNode right;


    public void setLR(TreeNode left, TreeNode right) {
        this.left = left;
        this.right = right;
    }

    //     1
//       /   \
//      2     3
//     / \   / \
//    4   5 6   7
    TreeNode root;

    @Before
    public void build() {
        TreeNode t1 = new TreeNode(); t1.setValue(1);
        TreeNode t2 = new TreeNode(); t2.setValue(2);
        TreeNode t3 = new TreeNode(); t3.setValue(3);
        TreeNode t4 = new TreeNode(); t4.setValue(4);
        TreeNode t5 = new TreeNode(); t5.setValue(5);
        TreeNode t6 = new TreeNode(); t6.setValue(6);
        TreeNode t7 = new TreeNode(); t7.setValue(7);
        t1.setLR(t2, t3);
        t2.setLR(t4, t5);
        t3.setLR(t6, t7);
        root = t1;

        TreeNode tt = new TreeNode(); tt.setValue(77);
        TreeNode tt1 = new TreeNode(); tt1.setValue(776);
        t5.setRight(tt);
        tt.setLeft(tt1);
    }

    /**
     * 行级别的输出
     */
    @Test
    public void printInOrder() {
        TreeNode current = root;
        Queue<TreeNode> queue = new ArrayBlockingQueue<TreeNode>(100);
        queue.add(current);
        while (!queue.isEmpty()) {
            int size = queue.size();
            for (int i = 0; i < size; i++) {
                TreeNode node = queue.poll();
                System.out.println(node.value);
                TreeNode left = node.left;
                TreeNode right = node.right;
                if(left != null) {
                    queue.add(left);
                }
                if(right != null) {
                    queue.add(right);
                }
            }
        }
    }


    @Test
    public void foreach() {
        print(root);
    }

    private void print(TreeNode cur) {
        if(cur == null) {
            return;
        }
        System.out.println(cur.value);
        print(cur.left);
        print(cur.right);
    }

    // 1、根据数组反推树
    // 2、 求路径

    @Test
    public void path() {
        int maxPath = getMaxPath(root, 0);
        System.out.println(maxPath);
    }

    // 左右想比
    private int getMaxPath(TreeNode root, int pos) {
        if(root == null) {
            return  pos;
        }
        pos ++;
        int lPos = getMaxPath(root.left, pos);
        int rPos = getMaxPath(root.right, pos);
        return Math.max(lPos, rPos);
    }


    private int getPathValue(TreeNode root, int pos) {
        if(root == null) {
            return  pos;
        }
        pos ++;
        int lPos = getMaxPath(root.left, pos);
        int rPos = getMaxPath(root.right, pos);
        return Math.max(lPos, rPos);
    }


    //     1
//       /   \
//      2     3
//     / \   / \
//    4   5 6   7
//         \
//         77
//        /
//        776
}

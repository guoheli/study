package com.leo.algorithm.array;

import lombok.Data;
import org.junit.Test;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 字节跳动 | 算法 | 215\. 数组中的第K个最大元素      | 2  |  --- [3,2,1,5,6,4] 和 k = 2
 * | 字节跳动 | 算法 | 560\. 和为K的子数组          | 1  |
 */
public class KMax {
    @Data
    static class Node {
        int val;
        Node next;
        Node front;

        @Override
        public String toString() {
            return "Node{" +
                    "val=" + val +
                    '}';
        }
    }

    Node head;
    Node tail;

    AtomicInteger count = new AtomicInteger(0);

    @Test
    public void getKMaxEle() {
        int[] arr = new int[]{3, 2, 1, 5, 6, 4, 6};

//        int[] arr = new int[]{3, 2, 3, 1, 2, 4, 5, 5, 6};

        int k = 2;
        for (int i : arr) {
            putNode(i, k);
        }
//        Node curr = head;
//        while (curr != null) {
//            if(--k == 0) {
//                System.out.println(curr.getVal() + "\t");
//                break;
//            }
//            curr = curr.next;
//        }
        if (tail != null) {
            System.out.println(tail.getVal());
        }
    }

    /**
     * {3, 2, 3, 1, 2, 4, 5, 5, 6}
     */
    public void putNode(int value, int k) {
        // insert
        insertNode(value);
        int high = count.getAndIncrement();
        // 如果个数超过了K个，则移除并重置(考虑特殊情况即最大值）
        if (high >= k) {
            Node tailFront = tail.front;
            tailFront.next = null;
            tail = tailFront;
        }
    }

    /**
     * 向有序链表插入数据 (进行测试)
     *
     * @param value
     */
    public void insertNode(int value) {
        if (head == null) {
            head = new Node();
            head.val = value;
            tail = head;
            return;
        }
        Node insertNode = new Node();
        insertNode.setVal(value);

        Node front = head.front;
        Node current = head;
        // 定位第一个最小值（考虑最后一位）
        while (current != null) {
            if (value >= current.val) {
                if (front != null) {
                    front.next = insertNode;
                } else {
                    //插入头部
                    head = insertNode;
                }
                insertNode.next = current;
                insertNode.front = front;
                current.front = insertNode;
                break;
            } else if (value == current.val) {
                // 相同值直接丢弃
                break;
            }
            front = current;
            current = current.next;
        }
        // 追加到尾部
        if (head != null && current == null) {
            front.next = insertNode;
            tail = insertNode;
            tail.front = front;
        }
    }

    @Test
    public void abs() {
        int[] arr = new int[]{3, 2, 1, 5, 6, 4, 4};
        for (int i : arr) {
            insertNode(i);
        }
        Node curr = head;
        System.out.println(head.val);
        while (curr != null) {
            System.out.println(curr.getVal() + "\t");
            curr = curr.next;
        }
        System.out.println(tail.val);
    }


    /**
     * 字数组和
     */
    @Test
    public void subSum() {
//        int[] array = new int[]{1, 1, 1}; int k = 2;
          int[] array = new int[]{3,4,7,2,-3,1,4,2}; int k = 7;
//        System.out.println(subarraySum(array, 2));
        System.out.println(optionSubarraySum(array, k));

    }

    public int subarraySum(int[] nums, int k) {
        int count = 0;
        for (int start = 0; start < nums.length; ++start) {
            int sum = 0;
            for (int end = start; end >= 0; --end) {
                sum += nums[end];
                if (sum == k) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * pre[i]=pre[i−1]+nums[i]
     *
     * 那么「[j..i][j..i] 这个子数组和为 kk 」这个条件我们可以转化为
     * pre[i]−pre[j−1]==k
     *
     * 简单移项可得符合条件的下标 jj 需要满足
     * pre[j−1]==pre[i]−k
     *
     *
     * @param nums
     * @param k
     * @return
     */
    public int optionSubarraySum(int[] nums, int k) {
        int count = 0, pre = 0;
        HashMap<Integer, Integer> mp = new HashMap<>();
        mp.put(0, 1);
        for (int i = 0; i < nums.length; i++) {
            pre += nums[i];
            if (mp.containsKey(pre - k)) {

                count += mp.get(pre - k);
            }
            // {0,1}-{3,1}-{7,1}-{14,2}-{16,1}-{13,1}
            mp.put(pre, mp.getOrDefault(pre, 0) + 1);
        }
        return count;
    }
}

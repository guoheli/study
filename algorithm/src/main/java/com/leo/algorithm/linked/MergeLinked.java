package com.leo.algorithm.linked;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 21. 合并两个有序链表
 * 将两个升序链表合并为一个新的 升序 链表并返回。新链表是通过拼接给定的两个链表的所有节点组成的。
 *
 *
 *
 * 示例 1：
 *
 *
 * 输入：l1 = [1,2,4], l2 = [1,3,4]
 * 输出：[1,1,2,3,4,4]
 * 示例 2：
 *
 * 输入：l1 = [], l2 = []
 * 输出：[]
 * 示例 3：
 *
 * 输入：l1 = [], l2 = [0]
 * 输出：[0]
 */
public class MergeLinked {


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class Item {
        Item next;
        int value;

        public Item(int value) {
            this.value = value;
        }
    }

    public static void main(String[] args) {
        // left
        Item l1 = new Item(1);
        Item l2 = new Item(2); l1.setNext(l2);
        Item l3 = new Item(4); l2.setNext(l3);

        // right
        Item r1 = new Item(1);
        Item r2 = new Item(3); r1.setNext(r2);
        Item r3 = new Item(4); r2.setNext(r3);


        Item source = l1;
        Item target = r1;

        mergeLinked(source, target);
        printOrdered(r1);

    }


    private static void printOrdered(Item item){
        if(item != null) {
            System.out.println(item.getValue());
            printOrdered(item.next);
        }
        return;
    }

    /**
     * 返回部位空的那个一个
     * @param source
     * @param target
     * @return
     */
    private static void mergeLinked(Item source, Item target) {
        // 目标长
        if(source == null) {
            return;
        }

        Item head = source;
        while(head != null) {
            Item next = head.next;
            head.next = null;
            // do some things
            insertAnyLinked(head, target);
            // 插入了最后端，则直接合并并返回(终结条件 Todo:1)
            if(head.next == null) {
                head.next = next;
                return;
            } else {
                target = head;
            }
            head = next;
        }
    }

    /**
     * 插入目标位置，并返回下一位
     * @param temp
     * @param target
     * @return
     */
    private static void insertAnyLinked(Item temp, Item target) {
        Item front = target;
        Item next = target.next;
        // 为空，直接插入后面，并返回
        while (next != null) {
            if(next.getValue() >= temp.getValue()) {
                front.next = temp;
                temp.next = next;
                return ;
            }
            front = next;
            next = next.next;
        }
        // 直接插入到最后面
        front.next = temp;
    }
}

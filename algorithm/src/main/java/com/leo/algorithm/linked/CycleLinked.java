package com.leo.algorithm.linked;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.Test;

/**
 * 判断链表是否存在环
 */
public class CycleLinked {

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

    /**
     * 单步、双步追赶
     */
    @Test
    public void test() {
        // left
        Item l1 = new Item(3);
        Item l2 = new Item(2); l1.setNext(l2);
        Item l3 = new Item(0); l2.setNext(l3);
        Item l4 = new Item(-4); l3.setNext(l4);
        l4.setNext(l3);
        // 3--> 2 --> 0 ----->4
        //            |______|
        System.out.println(existCycle(l1));
    }

    public boolean existCycle(Item head){
        Item next = head.next;
        // 必须2个以上才能成环
        if(next == null) {
            return false;
        }
        Item step1 = head;
        Item step2 = next;
        while(step1 != step2) {
            if(step1 == null || step2 == null) {
                return false;
            }
            step1 = step1.next;
            Item temp2 = step2.next;
            step2 = temp2 == null ? null : temp2.next;
        }
        return true;
    }
}

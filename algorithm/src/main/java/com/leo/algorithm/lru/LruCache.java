package com.leo.algorithm.lru;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 最近最少使用缓存机制
 * <p>
 * 实现 LRUCache 类：
 * <p>
 * LRUCache(int capacity) 以正整数作为容量 capacity 初始化 LRU 缓存
 * int get(int key) 如果关键字 key 存在于缓存中，则返回关键字的值，否则返回 -1 。
 * void put(int key, int value) 如果关键字已经存在，则变更其数据值；如果关键字不存在，则插入该组「关键字-值」。当缓存容量达到上限时，它应该在写入新数据之前删除最久未使用的数据值，从而为新的数据值留出空间。
 *  
 * <p>
 * 进阶：你是否可以在 O(1) 时间复杂度内完成这两种操作？
 * <p>
 *  
 * <p>
 * 示例：
 * <p>
 * 输入
 * ["LRUCache", "put", "put", "get", "put", "get", "put", "get", "get", "get"]
 * [[2], [1, 1], [2, 2], [1], [3, 3], [2], [4, 4], [1], [3], [4]]
 * 输出
 * [null, null, null, 1, null, -1, null, -1, 3, 4]
 * <p>
 * 解释
 * LRUCache lRUCache = new LRUCache(2);
 * lRUCache.put(1, 1); // 缓存是 {1=1}
 * lRUCache.put(2, 2); // 缓存是 {1=1, 2=2}
 * lRUCache.get(1);    // 返回 1
 * lRUCache.put(3, 3); // 该操作会使得关键字 2 作废，缓存是 {1=1, 3=3}
 * lRUCache.get(2);    // 返回 -1 (未找到)
 * lRUCache.put(4, 4); // 该操作会使得关键字 1 作废，缓存是 {4=4, 3=3}
 * lRUCache.get(1);    // 返回 -1 (未找到)
 * lRUCache.get(3);    // 返回 3
 * lRUCache.get(4);    // 返回 4
 *  
 * <p>
 * 提示：
 * <p>
 * 1 <= capacity <= 3000
 * 0 <= key <= 10000
 * 0 <= value <= 105
 * 最多调用 2 * 105 次 get 和 put
 * <p>
 * 来源：力扣（LeetCode）
 * 链接：https://leetcode-cn.com/problems/lru-cache
 * 著作权归领扣网络所有。商业转载请联系官方授权，非商业转载请注明出处。
 */
public class LruCache {


    int capacity = 0;

    public LruCache(int capacity) {
        this.capacity = capacity;
    }

    @Data
    @AllArgsConstructor
    class Item {
        /**
         * 待淘汰key
         */
        int key;
        Item next;

    }

    @Data
    @AllArgsConstructor
    class LruItem {
        int val;
        Item pre;
    }


    Item head =  new Item(-1, null);
    Item curPos = head;


    Map<Integer, LruItem> cacheDb = new HashMap<>();

    public int get(int key) {
        LruItem res = cacheDb.get(key);
        return res == null ? -1 : res.getVal();
    }

    public synchronized void put(int key, int value) {
        LruItem res = cacheDb.get(key);
        // 存在则替换更新位置
        if (res != null) {
            // [-1]-->[1]-->[2]
            // [-1]-X->[1]-X->[2] --> [-1]-->[2]-->[1]
            Item pre = res.pre;
            Item current = pre.next;
            // 修改的是最后一位，不做处理,反之移动到最后一位
            if (current != curPos) {
                Item next = current.next;
                pre.next = next;
                curPos.next = current;
                current.next = null;
            }
            cacheDb.put(key, new LruItem(value, curPos));
            curPos = current;
            return;
        }
        // 不存在的情况下, 如果容器满了，则进行淘汰
        if (cacheDb.size() == this.capacity) {
            Item delItem = head.next;
            Item next = delItem.next;
            int key1 = delItem.getKey();
            cacheDb.remove(key1);
            head.next = next;
            // 获取并修改下一个元素的pre
            LruItem lruItem = cacheDb.get(next.key);
            lruItem.pre = head;
            return;
        }
        Item item = new Item(key, null);
        curPos.next = item;
        cacheDb.put(key, new LruItem(value, curPos));
        curPos = item;
    }

    public static void main(String[] args) {
        LruCache lRUCache = new LruCache(2);
        lRUCache.put(1, 1); // 缓存是 {1=1}
        lRUCache.put(2, 2); // 缓存是 {1=1, 2=2}
        lRUCache.get(1);    // 返回 1
        lRUCache.put(3, 3); // 该操作会使得关键字 2 作废，缓存是 {1=1, 3=3}
        lRUCache.get(2);    // 返回 -1 (未找到)
        lRUCache.put(4, 4); // 该操作会使得关键字 1 作废，缓存是 {4=4, 3=3}
        lRUCache.get(1);    // 返回 -1 (未找到)
        lRUCache.get(3);    // 返回 3
        lRUCache.get(4);    // 返回 4
    }
}

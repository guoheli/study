package com.leo.algorithm.bloomFilter;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class Test {

    private static int size = 1000000;

    private static BloomFilter<Integer> bloomFilter = BloomFilter.create(Funnels.integerFunnel(), size);

    private static int dbSize = 32;

    private static String SPLIT = "&&VN";

    private static int DB_VIRTUAL = 256;
    private static String DB_PREFIX = "mysql_db_";
    private static String DEFAULT_DB = "mysql_db_0";
    private static List<String> REAL_NODES = new LinkedList<>();
    private static SortedMap<Integer, String> MAP = new TreeMap<>();

    static {
        for(int i = 0 ; i< dbSize; i++) {
            REAL_NODES.add(DB_PREFIX + i);
        }
        for(String server: REAL_NODES) {
            for(int i =0; i< DB_VIRTUAL; i++) {
                String virtualNode = server + SPLIT +i;
                MAP.put(getHash(virtualNode), virtualNode);
            }
        }
    }

    public static String getRealDb(String entry) {
        int hash = getHash(entry);
        SortedMap<Integer, String> subMap = MAP.tailMap(hash);
        if (!subMap.isEmpty()) {
            Integer index = subMap.firstKey();
            String virtualNode = subMap.get(index);
            return virtualNode.substring(0, virtualNode.indexOf(SPLIT));
        } else {
            return DEFAULT_DB;
        }
    }

    public static int getHash(String str) {
        final int p = 16777619;
        int hash = (int) 2166136261L;
        for (int i = 0; i < str.length(); i++) {
            hash = (hash ^ str.charAt(i)) * p;
        }
        hash += hash << 13;
        hash ^= hash >> 7;
        hash += hash << 3;
        hash ^= hash >> 17;
        hash += hash << 5;

        if (hash < 0) {
            hash = Math.abs(hash);
        }
        return hash;
    }



    public static void main(String[] args) {

        // instance
        String value = "630182853";
//        String value = "269586631";
        int iff = Math.abs((("IOT" +value).hashCode()) % 100);
        String realDb = getRealDb(value);
        System.out.println(realDb + " >>> "+iff);


        String deviceId = "2gFnDl3y";
        int collection = Math.abs(deviceId.hashCode()) % 100;
        System.out.println(collection);
//        MongoCollection mongoCollection = client.getDataBase().getCollection("shadow_deviceId_prefix_" + collection);


        if(true) {
            return;
        }

        for (int i = 0; i < size; i++) {
            bloomFilter.put(i);
        }

        long startTime = System.nanoTime(); // 获取开始时间
        //判断这一百万个数中是否包含29999这个数
        if (bloomFilter.mightContain(29999)) {
            System.out.println("命中了");
        }
        long endTime = System.nanoTime();   // 获取结束时间
        System.out.println("程序运行时间： " + (endTime - startTime) + "纳秒");
    }

}

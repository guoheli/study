//package com.leo.algorithm.face;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Vector;
//
//public class FeatureCompare {
//
//    static String  value1 = "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";
//
//    static String value2= "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";
//
//    public static void main(String[] args) {
//        Map<String, String> input = new HashMap<>();
//        input.put("a", value1);
//
//        Map<String, String> target = new HashMap<>();
//        target.put("a", value2);
//
//        new FeatureCompare().calculateVectorSimilarity(input, target);
//    }
//    //计算两个向量的相似度(cos)
//    public double calculateVectorSimilarity(Map<String, Object> inputFeatureVector , Map<String,Double> productFeatureVector){
//        double sumOfProduct = 0.0D;
//        double sumOfUser = 0.0D;
//        double sumOfSquare = 0.0D;
//        if(inputFeatureVector!=null && productFeatureVector!=null){
//            for(Map.Entry<String, Object> entry: inputFeatureVector.entrySet()){
//                String dimName = entry.getKey();
//                double dimScore = Double.parseDouble(entry.getValue().toString());
//                double itemDimScore = productFeatureVector.get(dimName);
//                sumOfUser += dimScore*dimScore;
//                sumOfProduct += itemDimScore*itemDimScore;
//                sumOfSquare += dimScore*itemDimScore;
//            }
//            if(sumOfUser*sumOfProduct==0.0D){
//                return 0.0D;
//            }
//            return sumOfSquare / (Math.sqrt(sumOfUser)*Math.sqrt(sumOfProduct));
//        }else {
//            return 0.0D;
//        }
//    }
//
//}

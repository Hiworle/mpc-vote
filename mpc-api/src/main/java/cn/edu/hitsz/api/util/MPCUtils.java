package cn.edu.hitsz.api.util;

import java.math.BigInteger;

public class MPCUtils {

    /**
     * 分割大整数
     *
     * @param result 投票的结果
     * @param k   k个候选人
     * @return 每个候选人的票数
     */
    public static int[] divide(BigInteger result, int k) {
        // 将BigInteger转换为字符串
        String numString = result.toString(2);

        // 计算每份的位数
        int numLength = numString.length();
        int partLength = numLength / k;

        // 创建存储结果的int数组
        int[] ret = new int[k];

        // 将每份的数字存储到数组中
        for (int i = 0; i < k; i++) {
            // 提取每份的起始和结束索引
            int startIndex = i * partLength;
            int endIndex = (i == k - 1) ? numLength : (i + 1) * partLength;

            // 截取每份的字符串并转换为int类型
            String partString = numString.substring(startIndex, endIndex);
            int partNum = Integer.parseInt(partString);

            // 将每份的数字存储到数组中
            ret[i] = partNum;
        }

        // 返回结果数组
        return ret;
    }


}

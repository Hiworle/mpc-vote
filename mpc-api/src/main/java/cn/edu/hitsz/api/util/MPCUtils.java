package cn.edu.hitsz.api.util;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MPCUtils {

    public static final int MAX_SCORE = 100;

    /**
     * 分割大整数
     *
     * @param result 投票的结果
     * @param n      n个候选人
     * @param m      m个投票人
     * @return 每个候选人的票数
     */
    public static int[] divide(BigInteger result,
                               int n,
                               int m) {
        // 将BigInteger转换为字符串
        String numString = result.toString(2);

        // 计算每份的位数
        int partLength = (int) (Math.log(m * MAX_SCORE) / Math.log(2)) + 1;

        // 创建存储结果的int数组
        int[] ret = new int[n];

        // 将每份的数字存储到数组中
        for (int i = 0; i < n; i++) {
            // 提取每份的起始和结束索引
            int startIndex = i * partLength;
            int endIndex = (i == n - 1) ? numString.length() : (i + 1) * partLength;

            // 截取每份的字符串并转换为int类型
            String partString = numString.substring(startIndex, endIndex);
            int partNum = Integer.parseInt(partString, 2);

            // 将每份的数字存储到数组中
            ret[i] = partNum;
        }

        // 返回结果数组
        return ret;
    }

    /**
     * 根据投票信息和投票人的数量生成大整数秘密
     */
    private static BigInteger generateSecret(List<Integer> voteList, int m) {

        // 比特长度
        int k = (int) (Math.log(m * MAX_SCORE) / Math.log(2)) + 1;

        BigInteger result = BigInteger.ZERO;
        for (int vote : voteList) {
            result = result.shiftLeft(k).add(BigInteger.valueOf(vote));
        }

        return result;
    }

    /**
     * 秘密分享
     */
    public static void secretShare(List<String> targets,
                                   List<Integer> voteList,
                                   String addr) throws UnsupportedEncodingException {

        List<BigInteger> secrets = randomSplit(generateSecret(voteList, targets.size()), targets.size());

        for (int i = 0; i < targets.size(); i++) {
            System.out.printf("Send [%s] to [%s]...\n", secrets.get(i).toString(2), targets.get(i));
            HttpUtils.httpPostRequest(
                    targets.get(i),
                    Map.of("data", secrets.get(i), "addr", addr)
            );
        }
    }

    /**
     * 将大整数随机分解成 m 份，他们的和为 secret
     */
    private static List<BigInteger> randomSplit(BigInteger number, int m) {
        Random random = new SecureRandom();
        List<BigInteger> delimiters = new ArrayList<>();
        delimiters.add(BigInteger.ZERO);
        for (int i = 0; i < m - 1; i++) {
            BigInteger delimiter = new BigInteger(number.bitLength(), random);
            delimiters.add(delimiter);
        }
        delimiters.add(number);

        List<BigInteger> result = new ArrayList<>();
        for (int i = 1; i < delimiters.size(); i++) {
            BigInteger left = delimiters.get(i - 1);
            BigInteger right = delimiters.get(i);
            BigInteger part = right.subtract(left);
            result.add(part);
        }

        return result;
    }
}

package cn.edu.hitsz.api.util;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 算法相关的工具类
 */
public class MPCUtils {

    public static final int MAX_SCORE = 100;

    /**
     * 分割大整数，用于将分发出去的秘密恢复，是 generateSecret() 的逆操作
     *
     * @param result 投票的结果
     * @param n      n个候选人
     * @param m      m个投票人
     * @return 每个候选人的票数
     */
    public static int[] recoverSecret(BigInteger result,
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
            int endIndex = numString.length() - i * partLength;
            int startIndex = Math.max(endIndex - partLength, 0);

            // 截取每份的字符串并转换为int类型
            String partString = numString.substring(startIndex, endIndex);
            int partNum = Integer.parseInt(partString, 2);

            // 将每份的数字存储到数组中
            ret[n - 1 - i] = partNum;
        }

        // 返回结果数组
        return ret;
    }

    /**
     * 根据投票信息和投票人的数量生成大整数秘密
     *
     * @param voteList 对各个候选人的投票信息
     * @param m        参与投票的人数
     * @return 秘密大整数
     */
    public static BigInteger generateSecret(int[] voteList, int m) {

        // 计算比特长度，该长度的作用是避免后续的算法中长度溢出
        int k = (int) (Math.log(m * MAX_SCORE) / Math.log(2)) + 1;

        // 将投票信息转换为大整数
        BigInteger result = BigInteger.ZERO;
        for (int vote : voteList) {
            result = result.shiftLeft(k).add(BigInteger.valueOf(vote));
        }

        return result;
    }

    /**
     * 秘密分享
     *
     * @param targets 目标地址
     * @param scores  投票信息，第一维度是项目，第二维度是候选人
     * @param addr    本机地址
     */
    public static void secretShare(List<String> targets,
                                   int[][] scores,
                                   String addr) throws UnsupportedEncodingException {


        // 将秘密随机分割，保证所有的子秘密的和为创建的 secret
        BigInteger[][] result = new BigInteger[scores.length][];
        for (int i = 0; i < scores.length; i++) {
            int[] score = scores[i];
            BigInteger[] subSecrets = randomSplit(generateSecret(score, targets.size()), targets.size());
            result[i] = subSecrets;
        }

        // 将子秘密分发给所有的投票者（含自己）
        for (int j = 0; j < result[0].length; j++) {
            List<BigInteger> toBeSent = new ArrayList<>();
            for (int i = 0; i < result.length; i++) {
                toBeSent.add(result[i][j]);
            }
            System.out.printf("Send [%s] to [%s]...\n", toBeSent, targets.get(j));
            HttpUtils.httpPostRequest(
                    targets.get(j),
                    Map.of("data", toBeSent, "addr", addr)
            );
        }
    }

    /**
     * 将大整数随机分解成 m 份，他们的和为 secret
     */
    private static BigInteger[] randomSplit(BigInteger number, int m) {
        Random random = new SecureRandom();
        BigInteger[] result = new BigInteger[m];
        BigInteger remain = number;
        for (int i = 0; i < m - 1; i++) {
            result[i] = new BigInteger(number.bitLength(), random);
            remain = remain.subtract(result[i]);
        }
        result[m - 1] = remain;

        return result;
    }
}

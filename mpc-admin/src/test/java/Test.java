import cn.edu.hitsz.api.util.MPCUtils;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

public class Test {

    public static void main(String[] args) throws UnsupportedEncodingException {
        MPCUtils.secretShare(List.of("1", "2"),
                new int[][]{{25, 29, 28}, {37, 35, 31}, {26, 18, 15}}
                , "1");

        int[] vote1 = MPCUtils.recoverSecret(BigInteger.valueOf(3753016), 3, 2);
        System.out.println("vote1 = " + Arrays.toString(vote1));
        int[] vote2 = MPCUtils.recoverSecret(BigInteger.valueOf(4998718), 3, 2);
        System.out.println("vote2 = " + Arrays.toString(vote2));
        int[] vote3 = MPCUtils.recoverSecret(BigInteger.valueOf(2761756), 3, 2);
        System.out.println("vote3 = " + Arrays.toString(vote3));
    }
}

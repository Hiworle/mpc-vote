package cn.edu.hitsz.api.entity;

import cn.edu.hitsz.api.entity.po.Candidate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoteResult {

    private Map<Integer, BigInteger[]> tallyMap;

    // 每个项目下求和结果（未分割）
    private BigInteger[] result;

    // 每个项目下得分情况，第一维是项目，第二维是候选人
    private int[][] detailedResult;

    // 每个参赛者的得分总和
    private int[] voteArray;

    private Candidate winner;
}

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

    private Map<Integer, BigInteger> tallyMap;

    private BigInteger result;

    private int[] voteArray;

    private Candidate winner;
}

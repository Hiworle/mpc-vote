package cn.edu.hitsz.admin.controller;

import cn.edu.hitsz.api.entity.ScoringItem;
import cn.edu.hitsz.api.entity.VoteResult;
import cn.edu.hitsz.api.entity.VoteStatus;
import cn.edu.hitsz.api.entity.po.Candidate;
import cn.edu.hitsz.api.entity.po.Voter;
import cn.edu.hitsz.api.util.HttpUtils;
import cn.edu.hitsz.api.util.MPCUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.IntStream;

@RestController
public class AdminController {

    // 投票人个数
    public static final int VOTER_COUNT = 3;

    // 投票人列表，正式环境应该从数据库中获取
    private static final Map<String, Voter> voterMap = new HashMap<>();

    // 候选人列表，正式环境从数据库中获取
    private static final List<Candidate> candidateList = List.of(
            Candidate.builder()
                    .id(0)
                    .name("张三")
                    .build(),
            Candidate.builder()
                    .id(1)
                    .name("李四")
                    .build(),
            Candidate.builder()
                    .id(2)
                    .name("王五")
                    .build()
    );

    public static final List<ScoringItem> scoringItems = List.of(
            ScoringItem.builder()
                    .id(0)
                    .name("创新型")
                    .description("评估参赛项目在人工智能应用领域的创新程度。考虑项目是否引入了新的思路、方法或技术，并且能够为现有问题提供新的解决方案。")
                    .score(30)
                    .build(),
            ScoringItem.builder()
                    .id(1)
                    .name("实用性")
                    .description("评估参赛项目在实际应用中的可行性和实用性。考虑项目是否能够解决实际问题，并且对现有或未来的人工智能应用具有积极的影响。")
                    .score(30)
                    .build(),
            ScoringItem.builder()
                    .id(2)
                    .name("技术复杂性")
                    .description("评估参赛项目所涉及的技术难度和复杂性。考虑项目是否在算法、模型设计或系统开发方面具有一定的挑战性，并且需要高水平的技术能力来实现。")
                    .score(20)
                    .build()
    );


    private static volatile VoteStatus status = VoteStatus.START;

    private static final Set<String> committedVoters = new HashSet<>();

    // 用于计票
    private static final Map<Integer, BigInteger[]> tallyMap = new HashMap<>();

    private static VoteResult voteResult = null;

    private final ObjectMapper mapper = new ObjectMapper();

    @GetMapping("/list-scoring-items")
    public List<ScoringItem> getScoringItemList() {
        return scoringItems;
    }


    @GetMapping("/list-voters")
    public Collection<Voter> getVoterList() {
        if (status == VoteStatus.START) {
            // 投票人未注册完毕，投票未开始
            return null;
        }
        return voterMap.values();
    }

    @PostMapping("/register")
    public Voter register(String addr) {
        Voter voter = Voter.builder()
                .id(voterMap.size())
                .addr(addr)
                .build();
        voterMap.putIfAbsent(addr, voter);
        if (voterMap.size() == VOTER_COUNT) {
            status = VoteStatus.VOTING;
        }
        return voter;
    }

    @GetMapping("/list-candidates")
    public List<Candidate> getCandidateList() {
        return candidateList;
    }

    // 投票人将秘密分发出去后，通过此接口通知管理中心
    @PostMapping("/vote-ok")
    public String voteOK(String fromAddr) {
        // 记录投票完成的投票人
        committedVoters.add(fromAddr);
        if (committedVoters.size() == voterMap.size()) {
            status = VoteStatus.TALLYING;
            voterMap.keySet().parallelStream().forEach(
                    addr -> {
                        try {
                            BigInteger[] tally = mapper.readValue(
                                    HttpUtils.httpPostRequest("http://" + addr + "/tally"),
                                    BigInteger[].class);
                            tallyMap.put(voterMap.get(addr).getId(), tally);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    }
            );

            // 计算结果
            voteResult = new VoteResult();
            voteResult.setTallyMap(tallyMap);
            List<BigInteger[]> itemTally = tallyMap.values().stream().toList();
            BigInteger[] result = new BigInteger[scoringItems.size()];
            for (int i = 0; i < scoringItems.size(); i++) {
                BigInteger sum = BigInteger.ZERO;
                for (BigInteger[] bigIntegers : itemTally) {
                    sum = sum.add(bigIntegers[i]);
                }
                result[i] = sum;
            }
            voteResult.setResult(result);
            int[][] detailedResult = new int[scoringItems.size()][];
            for (int i = 0; i < result.length; i++) {
                int[] divide = MPCUtils.divide(result[i], candidateList.size(), voterMap.size());
                detailedResult[i] = divide;
            }
            voteResult.setDetailedResult(detailedResult);

            // 计算参加者的得分总和情况
            int[] voteArray = new int[candidateList.size()];
            for (int i = 0; i < candidateList.size(); i++) {
                int sum = 0;
                for (int j = 0; j < detailedResult.length; j++) {
                    sum += detailedResult[j][i];
                }
                voteArray[i] = sum;
            }
            voteResult.setVoteArray(voteArray);

            // 计算胜者
            IntStream.range(0, voteArray.length)
                   .reduce((i, j) -> voteArray[i] > voteArray[j]? i : j)
                   .ifPresent(
                            i -> voteResult.setWinner(candidateList.get(i))
                    );

            status = VoteStatus.FINISHED;
            return "投票结束";
        } else {
            return "投票中";
        }
    }

    @GetMapping("/status")
    public VoteStatus getStatus() {
        return status;
    }

    @GetMapping("/result")
    public VoteResult getResult() {
        return voteResult;
    }

    @PostMapping("/reset")
    public String reset() {
        status = VoteStatus.START;
        voterMap.clear();
        committedVoters.clear();
        tallyMap.clear();
        voteResult = null;
        return "重置成功";
    }
}

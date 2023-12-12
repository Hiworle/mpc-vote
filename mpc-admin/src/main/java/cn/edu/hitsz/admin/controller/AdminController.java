package cn.edu.hitsz.admin.controller;

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

    private static volatile VoteStatus status = VoteStatus.START;

    private static final Set<String> committedVoters = new HashSet<>();

    // 用于计票
    private static final Map<Integer, BigInteger> tallyMap = new HashMap<>();

    private static VoteResult voteResult = null;

    private final ObjectMapper mapper = new ObjectMapper();

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
                            BigInteger tally = mapper.readValue(
                                    HttpUtils.httpPostRequest("http://" + addr + "/tally"),
                                    BigInteger.class);
                            tallyMap.put(voterMap.get(addr).getId(), tally);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    }
            );

            // 计算结果
            voteResult = new VoteResult();
            voteResult.setTallyMap(tallyMap);
            tallyMap.values().stream().reduce(BigInteger::add).ifPresent(
                    result -> {
                        voteResult.setResult(result);
                        int[] arr = MPCUtils.divide(result, candidateList.size(), voterMap.size());
                        IntStream.range(0, arr.length)
                                .reduce((i, j) -> arr[i] > arr[j] ? i : j)
                                .ifPresent(
                                        i -> {
                                            voteResult.setWinner(candidateList.get(i));
                                            voteResult.setVoteArray(arr);
                                        }
                                );
                    }
            );
            status = VoteStatus.FINISHED;
            return "投票结束";
        } else {
            return "投票中";
        }
    }

//    @PostMapping("/tally")
//    public String tally(HttpServletRequest request, BigInteger data) {
//        if (status != VoteStatus.TALLYING) {
//            return "计票还未开始";
//        }
//        Voter voter = voterMap.get(MPCUtils.parseAddr(request));
//        if (voter == null) return "投票人不存在";
//
//        tallyMap.put(voter.getId(), data);
//        if (tallyMap.size() == voterMap.size()) {
//            status = VoteStatus.FINISHED;
//            return "计票结束";
//        } else {
//            return "计票中";
//        }
//    }

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

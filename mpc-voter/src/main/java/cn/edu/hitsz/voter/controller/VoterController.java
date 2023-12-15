package cn.edu.hitsz.voter.controller;

import cn.edu.hitsz.api.entity.ScoringItem;
import cn.edu.hitsz.api.entity.VoteStatus;
import cn.edu.hitsz.api.entity.po.Voter;
import cn.edu.hitsz.api.util.HttpUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class VoterController {

    @Value("${mpc.admin.host}")
    private String adminHost;
    @Value("${server.port}")
    private String myPort;
    @Value("${server.address}")
    private String myHost;

    private final ObjectMapper mapper = new ObjectMapper();

    // 计票用的map，记录每个投票者发送的秘密
    private final Map<String, List<BigInteger>> secretMap = new HashMap<>();

    private volatile Set<String> uncommittedAddr;

    @PostMapping("/vote")
    public String vote(String data, String addr) throws JsonProcessingException {
        if (getStatus() != VoteStatus.VOTING) {
            return "投票阶段未开始/已结束";
        }

        // 获取投票者列表，不用每次都发请求
        BigInteger[] bigIntegers = mapper.readValue(data, BigInteger[].class);
        secretMap.put(addr, List.of(bigIntegers));


        return "Vote OK";
    }

    @PostMapping("/tally")
    public List<BigInteger> tally() throws JsonProcessingException {

        // todo 只接受管理中心的请求
        ScoringItem[] scoringItems = mapper.readValue(
                HttpUtils.httpGetRequest("http://" + adminHost + "/list-scoring-items"),
                ScoringItem[].class
        );

        // 对每个项目求和
        List<BigInteger> result = new ArrayList<>();
        List<List<BigInteger>> secretLists = secretMap.values().stream().toList();
        for (int i = 0; i < scoringItems.length; i++) {
            List<BigInteger> secretList = secretLists.get(i);
            BigInteger num = BigInteger.ZERO;
            for (int j = 0; j < secretLists.size(); j++) {
                num = num.add(secretList.get(j));
            }
            result.add(num);
        }
        return result;
    }

    private Set<String> getUncommittedAddr() throws JsonProcessingException {
        if (uncommittedAddr == null) {
            String json = HttpUtils.httpGetRequest(
                    "http://" + adminHost + "/list-voters"
            );
            return uncommittedAddr = Arrays.stream(mapper.readValue(json, Voter[].class))
                    .map(Voter::getAddr)
                    .collect(Collectors.toSet());
        }
        return uncommittedAddr;
    }

    private VoteStatus getStatus() throws JsonProcessingException {
        return mapper.readValue(
                HttpUtils.httpGetRequest("http://" + adminHost + "/status"),
                VoteStatus.class
        );
    }
}

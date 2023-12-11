package cn.edu.hitsz.voter.controller;

import cn.edu.hitsz.api.entity.VoteStatus;
import cn.edu.hitsz.api.entity.po.Voter;
import cn.edu.hitsz.api.util.HttpUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class VoterController {

    @Value("${mpc.admin.host}")
    private String adminHost;

    private final ObjectMapper mapper = new ObjectMapper();

    // 计票用的map，记录每个投票者发送的秘密
    private final Map<String, BigInteger> secretMap = new HashMap<>();

    private volatile Set<String> uncommittedAddr;

    @PostMapping("/vote")
    public String vote(BigInteger data, String addr) throws JsonProcessingException {
        if (getStatus() != VoteStatus.VOTING) {
            return "投票阶段未开始/已结束";
        }

        // 获取投票者列表，不用每次都发请求
        Set<String> uncommitted = getUncommittedAddr();
        if (uncommitted.contains(addr)) {
            secretMap.put(addr, data);
            uncommitted.remove(addr);
            if (uncommitted.isEmpty()) {
                HttpUtils.httpPostRequest(
                        "http://" + adminHost + "/vote-ok"
                );
            }
        }

        return "Vote OK";
    }

    @PostMapping("/tally")
    public BigInteger tally() {

        // todo 只接受管理中心的请求
        return secretMap.values().stream().reduce(BigInteger::add).orElse(BigInteger.ZERO);
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

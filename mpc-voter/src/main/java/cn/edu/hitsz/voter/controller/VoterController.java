package cn.edu.hitsz.voter.controller;

import cn.edu.hitsz.api.entity.VoteStatus;
import cn.edu.hitsz.api.entity.po.Voter;
import cn.edu.hitsz.api.util.HttpUtils;
import cn.edu.hitsz.api.util.MPCUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
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

    private final ObjectMapper mapper = new ObjectMapper();

    private volatile VoteStatus status;

    // 计票用的map，记录每个投票者发送的秘密
    private final Map<String, BigInteger> secretMap = new HashMap<>();

    private BigInteger result;

    private Set<String> uncommittedAddr;

    @PostMapping("/vote")
    public String vote(BigInteger data, HttpServletRequest request) throws JsonProcessingException {
        if (getStatus() != VoteStatus.VOTING) {
            return "投票阶段未开始/已结束";
        }

        // todo 获取投票者列表，其实可以不用每次都发请求
        Set<String> uncommitted = getUncommittedAddr();
        String addr = MPCUtils.parseAddr(request);
        if (uncommitted.contains(addr)) {
            secretMap.put(MPCUtils.parseAddr(request), data);
            uncommitted.remove(addr);
            if (uncommitted.isEmpty()) {
                HttpUtils.httpPostRequest(
                        "http://" + adminHost + "/vote-ok"
                );
            }
        }

        return "Vote OK";
    }

    private Set<String> getUncommittedAddr() throws JsonProcessingException {
        if (uncommittedAddr == null) {
            String json = HttpUtils.httpGetRequest(
                    "http://" + adminHost + "/list-voters"
            );
            return Arrays.stream(mapper.readValue(json, Voter[].class))
                    .map(Voter::getAddr)
                    .collect(Collectors.toSet());
        }
        return uncommittedAddr;
    }

    private VoteStatus getStatus() throws JsonProcessingException {
        return status = mapper.readValue(
                HttpUtils.httpGetRequest("http://" + adminHost + "/status"),
                VoteStatus.class
        );
    }
}

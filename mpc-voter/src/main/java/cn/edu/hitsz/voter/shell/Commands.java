package cn.edu.hitsz.voter.shell;

import cn.edu.hitsz.api.entity.VoteStatus;
import cn.edu.hitsz.api.entity.po.Candidate;
import cn.edu.hitsz.api.entity.po.Voter;
import cn.edu.hitsz.api.util.HttpUtils;
import cn.edu.hitsz.api.util.MPCUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

@ShellComponent
public class Commands {

    @Value("${mpc.admin.host}")
    private String adminHost;
    @Value("${server.port}")
    private String myPort;
    @Value("${server.address}")
    private String myHost;


    public static final int MAX_SCORE = 100;

    private final ObjectMapper mapper = new ObjectMapper();

    @ShellMethod(value = "Get vote status", key = {"status", "s"})
    public String status() {
        return HttpUtils.httpGetRequest(
                "http://" + adminHost + "/status"
        );
    }

    @ShellMethod(value = "Get voter list", key = {"list-voters", "lv"})
    public String listVoters() {

        return HttpUtils.httpGetRequest(
                "http://" + adminHost + "/list-voters"
        );
    }

    @ShellMethod(value = "Get candidate list", key = {"list-candidates", "lc"})
    public String listCandidates() {
        return HttpUtils.httpGetRequest(
                "http://" + adminHost + "/list-candidates"
        );
    }

    @ShellMethod(value = "Register as a voter", key = {"register", "r"})
    public String register() throws UnsupportedEncodingException {
        return HttpUtils.httpPostRequest(
                "http://" + adminHost + "/register",
                Map.of("addr", myHost + ':' + myPort)
        );
    }

    @ShellMethod(value = "Vote for a candidate", key = {"vote", "v"})
    public String vote(@ShellOption(arity = 100) List<Integer> voteList) throws JsonProcessingException, UnsupportedEncodingException {

        if (mapper.readValue(status(), VoteStatus.class) != VoteStatus.VOTING) {
            return "投票阶段未开始/已结束";
        }

        List<Candidate> candidates = List.of(mapper.readValue(listCandidates(), Candidate[].class));
        List<Voter> voters = List.of(
                mapper.readValue(listVoters(), Voter[].class)
        );

        if (candidates.size() != voteList.size()) {
            return "请输入完整的投票";
        }

        if (voteList.stream().anyMatch(i -> i < 0 || i > MAX_SCORE)) {
            return "请输入正确的投票";
        }

        // 秘密分享
        MPCUtils.secretShare(
                voters.stream().map(voter -> "http://" + voter.getAddr() + "/vote").toList(),
                voteList,
                myHost + ':' + myPort
        );

        return HttpUtils.httpPostRequest(
                "http://" + adminHost + "/vote-ok"
        );
    }

    @ShellMethod(value = "Tally votes", key = {"tally", "t"})
    public String tally(BigInteger data) throws JsonProcessingException {
        return HttpUtils.httpPostRequest(
                "http://" + adminHost + "/tally",
                mapper.writeValueAsString(data)
        );
    }

    @ShellMethod(value = "Get vote result", key = {"result", "res"})
    public String result() {
        return HttpUtils.httpGetRequest(
                "http://" + adminHost + "/result"
        );
    }
}

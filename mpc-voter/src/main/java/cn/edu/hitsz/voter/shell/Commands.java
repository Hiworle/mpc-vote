package cn.edu.hitsz.voter.shell;

import cn.edu.hitsz.api.entity.ScoringItem;
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

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.*;

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
    public String vote() throws JsonProcessingException, UnsupportedEncodingException {

        if (mapper.readValue(status(), VoteStatus.class) != VoteStatus.VOTING) {
            return "投票阶段未开始/已结束";
        }

        List<ScoringItem> scoringItems = List.of(mapper.readValue(listScoringItems(), ScoringItem[].class));
        List<Candidate> candidates = List.of(mapper.readValue(listCandidates(), Candidate[].class));
        List<Voter> voters = List.of(
                mapper.readValue(listVoters(), Voter[].class)
        );

        Scanner sc = new Scanner(System.in);
        // 第一维度是项目，第二维度是候选人
        int[][] itemCandidateScore = new int[scoringItems.size()][candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            System.out.println("对候选人 " + candidates.get(i).getName() + " 进行投票：");
            for (int j = 0; j < scoringItems.size(); j++) {
                System.out.printf("项目 %s 总分 %s：\n", scoringItems.get(j).getName(), scoringItems.get(j).getScore());
                itemCandidateScore[j][i] = sc.nextInt();
            }
        }

        // 秘密分享
        MPCUtils.secretShare(
                voters.stream().map(voter -> "http://" + voter.getAddr() + "/vote").toList(),
                itemCandidateScore,
                myHost + ':' + myPort
        );

        HttpUtils.httpPostRequest(
                "http://" + adminHost + "/vote-ok",
                Map.of("fromAddr", myHost + ':' + myPort)
        );

        return "投票成功，等待计票";
    }

    @ShellMethod(value = "List scoring items.", key = {"list-scoring-items", "lsi"})
    public String listScoringItems() {
        return HttpUtils.httpGetRequest(
                "http://" + adminHost + "/list-scoring-items"
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

package cn.edu.hitsz.admin.shell;

import cn.edu.hitsz.api.util.HttpUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

@ShellComponent
public class Commands {

    @Value("${mpc.admin.host}")
    private String adminHost;

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

    @ShellMethod(value = "Get vote result", key = {"result", "res"})
    public String result() {
        return HttpUtils.httpGetRequest(
                "http://" + adminHost + "/result"
        );
    }

    @ShellMethod(value = "Reset a vote", key = {"reset", "r"})
    public String reset() {
        String url = "http://" + adminHost + "/reset";
        return HttpUtils.httpPostRequest(url);
    }
}

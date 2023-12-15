package cn.edu.hitsz.api.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vote {

    /* 对各种评分项的投票结果 */
    List<Integer> votes;
}

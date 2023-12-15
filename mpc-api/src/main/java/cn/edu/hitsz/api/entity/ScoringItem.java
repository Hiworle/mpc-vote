package cn.edu.hitsz.api.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ScoringItem {

    private int id;

    private String name;

    private String description;

    private int score;
}

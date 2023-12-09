package cn.edu.hitsz.api.entity.po;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Candidate {

    private int id;

    private String name;

    private String description;
}

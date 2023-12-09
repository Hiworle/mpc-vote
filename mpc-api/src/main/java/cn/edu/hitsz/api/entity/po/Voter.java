package cn.edu.hitsz.api.entity.po;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Builder
@Accessors(chain = true)
public class Voter {

    private int id;

    private String addr;
}

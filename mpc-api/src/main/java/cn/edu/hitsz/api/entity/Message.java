package cn.edu.hitsz.api.entity;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigInteger;

@Data
public class Message implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;


    private BigInteger data;

}
